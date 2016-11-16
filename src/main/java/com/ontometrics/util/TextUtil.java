package com.ontometrics.util;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class TextUtil {
    /**
     * Resolves first parameter of url
     * @param url url
     * @param paramName query param
     * @return param value or <code>null</code> if it's absent
     * @throws UnsupportedEncodingException if url is invalid
     */
    public static String resolveUrlParameter(URL url, String paramName) throws UnsupportedEncodingException {
        final String[] pairs = url.getQuery().split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (paramName.equals(key)) {
                return idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            }
        }
        return null;
    }

}
