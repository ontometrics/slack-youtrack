package ontometrics.test.util;

import com.ontometrics.integrations.sources.InputStreamProvider;

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
    public InputStream openStream() throws IOException {
        return url.openStream();
    }
}
