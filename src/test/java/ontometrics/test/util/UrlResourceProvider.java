package ontometrics.test.util;

import com.ontometrics.integrations.sources.InputStreamHandler;
import com.ontometrics.integrations.sources.InputStreamProvider;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * UrlResourceProvider.java
 */
public class UrlResourceProvider implements InputStreamProvider {
    private URL url;

    private UrlResourceProvider(URL url) {
        this.url = url;
    }

    public static UrlResourceProvider instance (URL url){
        return new UrlResourceProvider(url);
    }

    @Override
    public <RES> RES openStream(InputStreamHandler<RES> inputStreamHandler) throws IOException {
        InputStream is = null;
        try {
            return inputStreamHandler.handleStream(is = url.openStream());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
