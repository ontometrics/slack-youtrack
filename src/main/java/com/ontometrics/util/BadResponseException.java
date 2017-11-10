package com.ontometrics.util;

import org.apache.http.client.HttpResponseException;

import java.net.URL;

/**
 * Signal about bad response from server
 */
public class BadResponseException extends HttpResponseException{
    private URL url;

    public BadResponseException(URL url, int responseCode) {
        this("Got response code "+responseCode+ " in response to "+url.toExternalForm(), url, responseCode);
    }

    public BadResponseException(String message, URL url, int responseCode) {
        super(responseCode, message);
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

}
