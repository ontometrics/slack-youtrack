package com.ontometrics.util;

import org.apache.http.HttpStatus;

import java.net.URL;

/**
 *
 */
public class HttpUtil {


    public static void checkResponseCode(int responseCode, URL requestUrl) throws BadResponseException {
        if (responseCode != HttpStatus.SC_OK){
            //we got not normal response from server
            throw new BadResponseException(requestUrl, responseCode);
        }
    }

}
