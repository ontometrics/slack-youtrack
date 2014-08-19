package ontometrics.integrations.sources;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEvent;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ProcessEventTest {

    @Test
    public void testGetID() throws Exception {

        ProcessEvent processEvent = new ProcessEvent.Builder()
                .issue(new Issue.Builder().projectPrefix("ASOC").id(28).build())
                .title("ASOC-28: User searches for Users by name")
                .link("http://ontometrics.com:8085/issue/ASOC-28")
                .description("lot of things changing here...")
                .published(new Date())
                .build();

        assertThat(processEvent.getIssue().toString(), is("ASOC-28"));

    }
}