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
}
