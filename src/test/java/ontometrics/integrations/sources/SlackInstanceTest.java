package ontometrics.integrations.sources;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;

/**
 * Tests for {@link com.ontometrics.integrations.configuration.SlackInstance} chat server
 * SlackInstanceTest.java
 */
public class SlackInstanceTest {
    private static final Logger logger = LoggerFactory.getLogger(SlackInstanceTest.class);
    private static final String SLACK_AUTH_TOKEN = "xoxp-2427064028-2427064030-2467602952-3d5dc6";


    @Test
    public void testThatChannelCanBePostedTo() throws UnsupportedEncodingException {
        String token = SLACK_AUTH_TOKEN;

        Client client = ClientBuilder.newClient();
        String slackUrl = "https://slack.com/api/";
        String channelPostPath = "chat.postMessage";

        String message = "hi there from unit test... {code} ";
        message = StringUtils.replaceChars(message, "{}", "[]");
//        message = message.replaceAll("}", "]");
        WebTarget slackApi = client.target(slackUrl).path(channelPostPath)
                .queryParam("token", token)
                .queryParam("text", message)
                .queryParam("channel", "#process");

        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        logger.info("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));

    }

    @Test
    public void testThatWeCanGetSlackUserList(){
        String token = SLACK_AUTH_TOKEN;

        Client client = ClientBuilder.newClient();
        String slackUrl = "https://slack.com/api/users.list?token=" + token;
        WebTarget slackApi = client.target(slackUrl);

        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        logger.info("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));
    }


}
