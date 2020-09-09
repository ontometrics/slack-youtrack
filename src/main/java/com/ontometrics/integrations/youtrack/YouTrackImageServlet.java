package com.ontometrics.integrations.youtrack;

import com.ontometrics.db.MapDb;
import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.configuration.StreamProviderFactory;
import com.ontometrics.integrations.configuration.YouTrackInstance;
import com.ontometrics.integrations.configuration.YouTrackInstanceFactory;
import com.ontometrics.integrations.sources.InputStreamHandler;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 *
 */
public class YouTrackImageServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(YouTrackImageServlet.class);

    private YouTrackInstance youTrackInstance;
    /**
     * Attachment URL example "http://issuetracker.com/_persistent/image.png?file=78-496"
     */
    private StreamProvider streamProvider;

    @Override
    public void init() throws ServletException {
        super.init();
        final Configuration configuration = ConfigurationFactory.get();

        youTrackInstance = YouTrackInstanceFactory.createYouTrackInstance(configuration);
        streamProvider = StreamProviderFactory.createStreamProvider(configuration);

    }

    @Override
    protected void service(HttpServletRequest servletRequest, final HttpServletResponse servletResponse)
            throws ServletException, IOException {
        if (!servletRequest.getMethod().equals(HttpGet.METHOD_NAME)){
            servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Only GET method supported");
            return;
        }

        String imageId = servletRequest.getParameter("rid");
        if (StringUtils.isBlank(imageId)){
            servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Resource id not specified");
            return;
        }

        URL youTrackUrl = resolveYouTrackAttachmentUri(imageId);
        logger.debug("Generated URI {0}", youTrackUrl);

        try {
            String name = servletRequest.getParameter("name");
            if (StringUtils.isNotBlank(name)) {
                servletResponse.addHeader("Content-Disposition", buildContentDisposition(CONTENT_DISPOSITION_ATTACHMENT, name, Charset.forName("UTF-8")));
            }
            streamProvider.openResourceStream(youTrackUrl, new InputStreamHandler<Void>() {
                @Override
                public Void handleStream(InputStream is, int responseCode) throws Exception {
                    servletResponse.setStatus(responseCode);
                    copyStream(is, servletResponse);
                    return null;
                }
            });
        } catch (Exception e) {
            logger.error("Failed to process request", e);
            servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

    private void copyStream(InputStream is, HttpServletResponse servletResponse) {
        //TODO we may generate thumbnail here if needed, for now we just stream it back
        try {
            IOUtils.copy(is, servletResponse.getOutputStream());
        } catch (IOException e) {
            logger.error("Failed to write response", e);
        }
    }


    private URL resolveYouTrackAttachmentUri(String externalImageId) throws MalformedURLException {
        String youTrackImageId = resolveYouTrackImageId(externalImageId);
        if (StringUtils.isBlank(externalImageId)) {
            throw new RuntimeException("Image not found");
        }
        return new URL(String.format("%s/_persistent/%s?file=%s", youTrackInstance.getBaseUrl(), "image.png",
                youTrackImageId));
    }

    @SuppressWarnings("unused")
    private String resolveYouTrackImageId(String externalImageId) {
        return MapDb.instance().getAttachmentMap().get(externalImageId);
    }


    public static final String CONTENT_DISPOSITION_INLINE = "inline";
    public static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment";

    /**
     * Set the (new) value of the {@code Content-Disposition} header
     * for {@code main body}, optionally encoding the filename using the RFC 5987.
     * <p>Only the US-ASCII, UTF-8 and ISO-8859-1 charsets are supported.
     *
     * @param type content disposition type
     * @param filename the filename (may be {@code null})
     * @param charset the charset used for the filename (may be {@code null})
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.4">RFC 7230 Section 3.2.4</a>
     * @since 4.3.3
     */
    private static String buildContentDisposition(String type, String filename, Charset charset){

        if (!CONTENT_DISPOSITION_INLINE.equals(type) && !CONTENT_DISPOSITION_ATTACHMENT.equals(type)) {
            throw new IllegalArgumentException("type must be inline or attachment");
        }

        StringBuilder builder = new StringBuilder(type);
        if (filename != null) {
            builder.append("; ");

            if (charset == null || charset.name().equals("US-ASCII")) {
                builder.append("filename=\"");
                builder.append(filename).append('\"');
            } else {
                builder.append("filename*=");
                builder.append(encodeHeaderFieldParam(filename, charset));
            }
        }
        return builder.toString();
    }

    /**
     * Copied from Spring  {@link org.springframework.http.HttpHeaders}
     *
     * Encode the given header field param as describe in RFC 5987.
     *
     * @param input the header field param
     * @param charset the charset of the header field param string
     * @return the encoded header field param
     * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
     */
    private static String encodeHeaderFieldParam(String input, Charset charset) {
        if (charset.name().equals("US-ASCII")) {
            return input;
        }
        byte[] source = input.getBytes(charset);
        int len = source.length;
        StringBuilder sb = new StringBuilder(len << 1);
        sb.append(charset.name());
        sb.append("''");
        for (byte b : source) {
            if (isRFC5987AttrChar(b)) {
                sb.append((char) b);
            } else {
                sb.append('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                sb.append(hex1);
                sb.append(hex2);
            }
        }
        return sb.toString();
    }

    /**
     * Copied from Spring  {@link org.springframework.http.HttpHeaders}
     */
    private static boolean isRFC5987AttrChar(byte c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
                c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
    }
}
