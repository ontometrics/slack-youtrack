package ontometrics.integrations.sources;

import com.ontometrics.integrations.sources.ExternalStreamProvider;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/** Test for {@link com.ontometrics.integrations.sources.ExternalStreamProvider}
 * ExternalResourceInputStreamProviderTest.java
 */
public class ExternalStreamProviderTest {

    @Test
    public void testExternalResourceWorks() throws IOException {
        String res = new ExternalStreamProvider("http://ya.ru/").openResourceStream(IOUtils::toString);
        assertThat(res, notNullValue());
        assertThat(res.length(), not(is(0)));
    }
}
