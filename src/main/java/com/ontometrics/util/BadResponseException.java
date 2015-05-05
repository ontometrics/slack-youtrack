package com.ontometrics.util;

import java.net.URL;

/**
 * Signal about bad response from server
 */
public class BadResponseException extends RuntimeException {
    private URL url;
    private int responseCode;

    public BadResponseException(URL url, int responseCode) {
        this("Got response code "+responseCode+ " in response to "+url.toExternalForm(), url, responseCode);
    }

    public BadResponseException(String message, URL url, int responseCode) {
        super(message);
        this.url = url;
        this.responseCode = responseCode;
    }

    public URL getUrl() {
        return url;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
