package ontometrics.integrations.sources;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEvent;
import org.junit.Test;

import java.net.URL;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ProcessEventTest {

    @Test
    public void testGetID() throws Exception {
        Issue issue = new Issue.Builder().projectPrefix("ASOC").id(28)
                .title("ASOC-28: User searches for Users by name")
                .link(new URL("http://ontometrics.com:8085/issue/ASOC-28"))
                .description("lot of things changing here...")
                .build();
        ProcessEvent processEvent = new ProcessEvent.Builder()
                .issue(issue)
                .published(new Date())
                .build();

        assertThat(processEvent.getIssue().toString(), is("ASOC-28"));

    }
}