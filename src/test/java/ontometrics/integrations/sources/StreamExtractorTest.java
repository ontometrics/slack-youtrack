package ontometrics.integrations.sources;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.ontometrics.integrations.configuration.IssueTracker;
import com.ontometrics.integrations.events.Issue;
import ontometrics.test.util.TestUtil;
import org.junit.Test;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * User: robwilliams
 * Date: 8/20/14
 * Time: 9:09 PM
 * <p/>
 * (c) ontometrics 2014, All Rights Reserved
 */
public class StreamExtractorTest {

    private Logger log = getLogger(StreamExtractorTest.class);
    private MockIssueTracker mockYouTrackInstance;

    @Test
    public void testReadingExtraction() throws IOException, XMLStreamException {
        mockYouTrackInstance = new MockIssueTracker("/feeds/issues-feed-rss.xml", "/feeds/issue-changes.xml");

        Issue issue = new Issue.Builder().projectPrefix("ASOC").id(48).build();
        InputStream inputStream = mockYouTrackInstance.getChangesUrl(issue).openStream();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);

        boolean extractingChange = false;
        while (eventReader.hasNext()) {
            XMLEvent nextEvent = eventReader.nextEvent();
            switch (nextEvent.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement startElement = nextEvent.asStartElement();
                    String elementName = startElement.getName().getLocalPart();
                    if (elementName.equals("change")) {
                        log.info("extracting change");
                        extractingChange = true;
                    } else {
                        if (extractingChange){
                            log.info("found tag: {}", elementName);
                        }

                    }
                    break;

                case XMLStreamConstants.ATTRIBUTE:
                    break;


                case XMLStreamConstants.END_ELEMENT:
                    EndElement endElement = nextEvent.asEndElement();
                    String tagName = endElement.getName().getLocalPart();
                    if (tagName.equals("change")){
                        log.info("got changes");
                        extractingChange = false;
                    }
                    break;

            }
        }

    }

    private static class MockIssueTracker implements IssueTracker {
        private String feedUrl;
        private String changesUrl;

        private MockIssueTracker(String feedUrl, String changesUrl) {
            this.feedUrl = feedUrl;
            this.changesUrl = changesUrl;
        }

        @Override
        public URL getBaseUrl() {
            return null;
        }

        @Override
        public URL getFeedUrl() {
            return TestUtil.getFileAsURL(feedUrl);
        }

        @Override
        public URL getChangesUrl(Issue issue) {
            return TestUtil.getFileAsURL(changesUrl);
        }
    }
}


