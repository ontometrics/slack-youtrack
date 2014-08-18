package ontometrics.test.util;

import java.net.URL;

/**
 * Created by rob on 7/14/14.
 */
public class TestUtil {

        public static URL getFileAsURL(String path) {
            URL url = TestUtil.class.getResource(path);
            if (url == null && !path.startsWith("/resources")) {
                url = TestUtil.class.getResource("/resources"+path);
            }
            return url;
        }

    }
