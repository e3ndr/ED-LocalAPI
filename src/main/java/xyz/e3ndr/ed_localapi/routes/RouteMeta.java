package xyz.e3ndr.ed_localapi.routes;

import java.io.IOException;

import co.casterlabs.rhs.protocol.StandardHttpStatus;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.sora.api.http.HttpProvider;
import co.casterlabs.sora.api.http.SoraHttpSession;
import co.casterlabs.sora.api.http.annotations.HttpEndpoint;
import xyz.e3ndr.ed_localapi.Main;

public class RouteMeta extends Helper implements HttpProvider {

    @HttpEndpoint(uri = "/*")
    public HttpResponse onGetIndex(SoraHttpSession session) throws IOException, InterruptedException {
        return addCors(
            HttpResponse.newFixedLengthResponse(
                StandardHttpStatus.OK,
                String.format(
                    "Success! If you're seeing this, ED-LocalAPI is working correctly! Just direct your favorite tool to connect to %s:%d :^)",
                    Main.HOSTNAME.contains(":") ? '[' + Main.HOSTNAME + ']' : Main.HOSTNAME, // Correct the formatting for ipv6 urls.
                    Main.PORT
                )
            ).setMimeType("text/plain")
        );
    }

    @HttpEndpoint(uri = "/edla/challenge/:toEcho")
    public HttpResponse onChallenge(SoraHttpSession session) throws IOException, InterruptedException {
        return addCors(
            HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, session.getUriParameters().get("toEcho"))
                .setMimeType("text/plain")
        );
    }

}
