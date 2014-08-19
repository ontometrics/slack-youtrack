package ontometrics.integrations.sources;

import com.ontometrics.integrations.sources.ExternalResourceInputStreamProvider;
import com.ontometrics.integrations.sources.InputStreamHandler;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/** Test for {@link com.ontometrics.integrations.sources.ExternalResourceInputStreamProvider}
 * ExternalResourceInputStreamProviderTest.java
 */
public class ExternalResourceInputStreamProviderTest {

    @Test
    public void testExternalResourceWorks() throws IOException {
        String res = new ExternalResourceInputStreamProvider("http://ya.ru/").openStream(IOUtils::toString);
        assertThat(res, notNullValue());
        assertThat(res.length(), not(is(0)));
    }
}
