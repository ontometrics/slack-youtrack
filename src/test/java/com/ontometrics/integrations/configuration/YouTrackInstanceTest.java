package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class YouTrackInstanceTest {

    private YouTrackInstance youTrackInstance;

    @Before
    public void setup(){
        youTrackInstance = new YouTrackInstance.Builder().baseUrl("http://ontometrics.com:8085").build();
    }

    @Test
    public void testGetBaseUrl() throws Exception {
        assertThat(youTrackInstance.getBaseUrl(), is(new URL("http://ontometrics.com:8085")));
    }

    /*
    @Test
    public void testGetFeedUrl() throws Exception {
        assertThat(youTrackInstance.getFeedUrl(), is(new URL("http://ontometrics.com:8085/_rss/issues")));
    }
*/
    @Test
    public void testGetChangesUrl() throws Exception {
        assertThat(youTrackInstance.getChangesUrl(new Issue.Builder().projectPrefix("ASOC").id(505).build()),
                is(new URL("http://ontometrics.com:8085/rest/issue/ASOC-505/changes")));
    }

    @Test
    public void testAttachmentsUrlBuilder() throws MalformedURLException {
        assertThat(youTrackInstance.getAttachmentsUrl(new Issue.Builder().projectPrefix("ASOC").id(480).build()),
                is(new URL("http://ontometrics.com:8085/rest/issue/ASOC-480/attachment")));
    }

    @Test
    public void testThatNoPortWorks() throws MalformedURLException {
        youTrackInstance = new YouTrackInstance.Builder().baseUrl("http://ontometrics.com").build();

        assertThat(youTrackInstance.getBaseUrl(), is(new URL("http://ontometrics.com")));
    }

}