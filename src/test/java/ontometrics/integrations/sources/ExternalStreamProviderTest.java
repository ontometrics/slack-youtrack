package ontometrics.integrations.sources;

import com.ontometrics.integrations.sources.AuthenticatedHttpStreamProvider;
import com.ontometrics.integrations.sources.Authenticator;
import com.ontometrics.integrations.sources.InputStreamHandler;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Executor;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for {@link com.ontometrics.integrations.sources.AuthenticatedHttpStreamProvider}
 * ExternalResourceInputStreamProviderTest.java
 */
public class ExternalStreamProviderTest {

    @Test
    public void testExternalResourceWorks() throws Exception {
        String res;
        res = new AuthenticatedHttpStreamProvider(new Authenticator() {
            @Override
            public void authenticate(Executor httpExecutor) {
            }
        }).openResourceStream(new URL("http://ya.ru"), new InputStreamHandler<String>() {
            @Override
            public String handleStream(InputStream is, int responseCode) throws Exception {
                return IOUtils.toString(is);
            }
        });
        assertThat(res, notNullValue());
        assertThat(res.length(), not(is(0)));
    }
}
