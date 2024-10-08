package xyz.e3ndr.ed_localapi.routes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rhs.protocol.StandardHttpStatus;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.rhs.session.Websocket;
import co.casterlabs.rhs.session.WebsocketListener;
import co.casterlabs.rhs.util.DropConnectionException;
import co.casterlabs.sora.api.http.HttpProvider;
import co.casterlabs.sora.api.http.SoraHttpSession;
import co.casterlabs.sora.api.http.annotations.HttpEndpoint;
import co.casterlabs.sora.api.websockets.SoraWebsocketSession;
import co.casterlabs.sora.api.websockets.WebsocketProvider;
import co.casterlabs.sora.api.websockets.annotations.WebsocketEndpoint;
import lombok.SneakyThrows;
import xyz.e3ndr.ed_localapi.EliteDangerous;
import xyz.e3ndr.ed_localapi.EliteDangerous.ContentListener;

public class RouteFile implements HttpProvider, WebsocketProvider {

    @HttpEndpoint(uri = "/file/:file")
    public HttpResponse onGetFileContent(SoraHttpSession session) throws FileNotFoundException {
        String file = session.getUriParameters().get("file");

        if (!EliteDangerous.isGameRunning) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.FAILED_DEPENDENCY, "Game is not running")
                .setMimeType("text/plain");
        }

        if (!EliteDangerous.getActiveFiles().contains(file)) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NOT_FOUND, "File not found")
                .setMimeType("text/plain");
        }

        if (file.equals("Journal")) {
            return HttpResponse.newFixedLengthFileResponse(StandardHttpStatus.OK, EliteDangerous.currentJournal)
                .setMimeType("application/json; charset=utf-8");
        } else {
            return HttpResponse.newFixedLengthFileResponse(StandardHttpStatus.OK, new File(EliteDangerous.GAME_DIR, file + ".json"))
                .setMimeType("application/json; charset=utf-8");
        }
    }

    @HttpEndpoint(uri = "/files")
    public HttpResponse onListFiles(SoraHttpSession session) {
        if (!EliteDangerous.isGameRunning) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.FAILED_DEPENDENCY, "Game is not running").setMimeType("text/plain");
        }

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, Rson.DEFAULT.toJson(EliteDangerous.getActiveFiles()))
            .setMimeType("application/json; charset=utf-8");
    }

    @WebsocketEndpoint(uri = "/file/:file")
    public WebsocketListener onStreamContent(SoraWebsocketSession session) throws IOException, InterruptedException {
        String file = session.getUriParameters().get("file");

        if (!EliteDangerous.isGameRunning) {
            throw new DropConnectionException();
        }

        if (!EliteDangerous.getActiveFiles().contains(file)) {
            throw new DropConnectionException();
        }

        return new WebsocketListener() {
            private Websocket websocket;
            private ContentListener listener = new ContentListener() {
                @Override
                public void onGameClose() {
                    try {
                        websocket.close();
                    } catch (IOException ignored) {}
                }

                @Override
                public void onChange(JsonObject newContent) {
                    try {
                        websocket.send(newContent.toString());
                    } catch (IOException ignored) {
                        try {
                            websocket.close();
                        } catch (IOException ignored2) {}
                    }
                }
            };

            @SneakyThrows
            @Override
            public void onOpen(Websocket websocket) {
                this.websocket = websocket;
                EliteDangerous.registerFileListener(file, this.listener);

                if (!file.equals("Journal")) {
                    // Get the file contents and send it.
                    websocket.send(Files.readString(new File(EliteDangerous.GAME_DIR, file + ".json").toPath()));
                }
            }

            @Override
            public void onClose(Websocket websocket) {
                EliteDangerous.unregisterFileListener(file, this.listener);
            }
        };
    }

}
