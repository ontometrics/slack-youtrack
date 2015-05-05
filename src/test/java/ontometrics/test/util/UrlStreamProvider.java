package ontometrics.test.util;

import com.ontometrics.integrations.sources.InputStreamHandler;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;

import java.io.InputStream;
import java.net.URL;

/**
 * URL resource provider, opens resource using {@link java.net.URL#openStream()}
 *
 * UrlResourceProvider.java
 */
public class UrlStreamProvider implements StreamProvider {

    private UrlStreamProvider() {}

    public static UrlStreamProvider instance (){
        return new UrlStreamProvider();
    }

    @Override
    public <RES> RES openResourceStream(URL resourceUrl, InputStreamHandler<RES> inputStreamHandler) throws Exception {
        InputStream is = null;
        try {
            return inputStreamHandler.handleStream(is = resourceUrl.openStream(), HttpStatus.SC_OK);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
