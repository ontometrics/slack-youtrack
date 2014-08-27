package ontometrics.test.util;

import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.util.DateBuilder;

import java.net.URL;
import java.util.Date;

/**
 * Created by rob on 7/14/14.
 */
public class TestUtil {

    public static URL getFileAsURL(String path) {
        URL url = TestUtil.class.getResource(path);
        if (url == null && !path.startsWith("/resources")) {
            url = TestUtil.class.getResource("/resources" + path);
        }
        return url;
    }

    /**
     * Sets {@link com.ontometrics.integrations.configuration.EventProcessorConfiguration#getIssueHistoryWindowInMinutes()}
     * setting such value which will be enough to fetch all events from the feed
     */
    public static void setIssueHistoryWindowSettingToCoverAllIssues() {
        Date oldestIssueDate = new DateBuilder().year(2013).build();
        final int offsetInMinutes = (int) ((new Date().getTime() - oldestIssueDate.getTime()) / 1000 / 60) + 5;
        EventProcessorConfiguration.instance().setIssueHistoryWindowInMinutes(offsetInMinutes);
    }
}
