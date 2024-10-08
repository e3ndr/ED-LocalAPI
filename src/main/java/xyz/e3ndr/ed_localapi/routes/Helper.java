package xyz.e3ndr.ed_localapi.routes;

import co.casterlabs.rhs.server.HttpResponse;

class Helper {

    static HttpResponse addCors(HttpResponse response) {
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.putHeader("Access-Control-Allow-Methods", "GET");
        response.putHeader("Access-Control-Allow-Headers", "Authorization, *");

        return response;
    }

}
