package xyz.e3ndr.ed_localapi.routes;

import java.io.FileNotFoundException;
import java.io.IOException;

import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rhs.protocol.StandardHttpStatus;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.rhs.session.Websocket;
import co.casterlabs.rhs.session.WebsocketListener;
import co.casterlabs.sora.api.http.HttpProvider;
import co.casterlabs.sora.api.http.SoraHttpSession;
import co.casterlabs.sora.api.http.annotations.HttpEndpoint;
import co.casterlabs.sora.api.websockets.SoraWebsocketSession;
import co.casterlabs.sora.api.websockets.WebsocketProvider;
import co.casterlabs.sora.api.websockets.annotations.WebsocketEndpoint;
import xyz.e3ndr.ed_localapi.EliteDangerous;
import xyz.e3ndr.ed_localapi.EliteDangerous.GameListener;

public class RouteGame implements HttpProvider, WebsocketProvider {
    private static final String GAME_OPEN = JsonObject.singleton("isGameRunning", true).toString();
    private static final String GAME_CLOSE = JsonObject.singleton("isGameRunning", false).toString();

    @HttpEndpoint(uri = "/game")
    public HttpResponse onGetGameState(SoraHttpSession session) throws FileNotFoundException {
        return HttpResponse.newFixedLengthResponse(
            StandardHttpStatus.OK,
            new JsonObject()
                .put("isGameRunning", EliteDangerous.isGameRunning)
        ).setMimeType("application/json; charset=utf-8");
    }

    @WebsocketEndpoint(uri = "/game")
    public WebsocketListener onStreamGameState(SoraWebsocketSession session) throws IOException, InterruptedException {
        return new WebsocketListener() {
            private Websocket websocket;
            private GameListener listener = new GameListener() {
                @Override
                public void onGameOpen() {
                    try {
                        websocket.send(GAME_OPEN);
                    } catch (IOException ignored) {
                        try {
                            websocket.close();
                        } catch (IOException ignored2) {}
                    }
                }

                @Override
                public void onGameClose() {
                    try {
                        websocket.send(GAME_CLOSE);
                    } catch (IOException ignored) {
                        try {
                            websocket.close();
                        } catch (IOException ignored2) {}
                    }
                }
            };

            @Override
            public void onOpen(Websocket websocket) {
                this.websocket = websocket;
                EliteDangerous.registerGameListener(this.listener);

                if (EliteDangerous.isGameRunning) {
                    this.listener.onGameOpen();
                } else {
                    this.listener.onGameClose();
                }
            }

            @Override
            public void onClose(Websocket websocket) {
                EliteDangerous.unregisterGameListener(this.listener);
            }
        };
    }

}
