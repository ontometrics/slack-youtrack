package com.ontometrics.integrations.sources;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * ExternalStreamProvider.java
 */
public class NonAuthenticatedHttpStreamProvider implements StreamProvider {

    private static final Logger logger = LoggerFactory.getLogger(NonAuthenticatedHttpStreamProvider.class);

    private Executor httpExecutor;
    public NonAuthenticatedHttpStreamProvider() {
        this.httpExecutor = Executor.newInstance();
    }

    /**
     * @throws IOException
     */
    @Override
    public <RES> RES openResourceStream(URL resourceUrl, final InputStreamHandler<RES> inputStreamHandler) throws Exception {
        Request request = Request.Get(resourceUrl.toExternalForm());
        return openResourceStream(request, inputStreamHandler, httpExecutor);
    }

    public static <RES> RES openResourceStream(Request request, final InputStreamHandler<RES> inputStreamHandler, Executor httpExecutor) throws Exception {
        return httpExecutor.execute(request)
                .handleResponse(
                        new ResponseHandler<RES>() {
                            @Override
                            public RES handleResponse(HttpResponse httpResponse) throws IOException {
                                try {
                                    StatusLine statusLine = httpResponse.getStatusLine();
                                    if (StringUtils.isNotBlank(statusLine.getReasonPhrase())) {
                                        logger.debug("Got response with code {} reason: {}", statusLine.getStatusCode(), statusLine.getReasonPhrase());
                                    } else {
                                        logger.debug("Got response with code {}", statusLine.getStatusCode());
                                    }
                                    return inputStreamHandler.handleStream(httpResponse.getEntity().getContent(), statusLine.getStatusCode());
                                } catch (IOException e) {
                                    throw e;
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                );

    }
}
