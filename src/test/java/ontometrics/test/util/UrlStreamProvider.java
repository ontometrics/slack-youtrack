package ontometrics.test.util;

import com.ontometrics.integrations.sources.InputStreamHandler;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * URL resource provider, opens resource using {@link java.net.URL#openStream()}
 *
 * UrlResourceProvider.java
 */
public class UrlStreamProvider implements StreamProvider {
    private URL url;

    private UrlStreamProvider(URL url) {
        this.url = url;
    }

    public static UrlStreamProvider instance (URL url){
        return new UrlStreamProvider(url);
    }

    @Override
    public <RES> RES openResourceStream(InputStreamHandler<RES> inputStreamHandler) throws IOException {
        InputStream is = null;
        try {
            return inputStreamHandler.handleStream(is = url.openStream());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
