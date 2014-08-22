package ontometrics.integrations.sources;

import com.ontometrics.integrations.configuration.IssueTracker;
import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.ProcessEventChange;
import com.ontometrics.integrations.events.ProcessEventChangeSet;
import ontometrics.test.util.TestUtil;
import org.junit.Test;
import org.slf4j.Logger;

import javax.xml.namespace.QName;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
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
        List<ProcessEventChange> changes = extractFields();
        assertThat(changes.size(), is(not(0)));
        mockYouTrackInstance = new MockIssueTracker("/feeds/issues-feed-rss.xml", "/feeds/issue-changes2.xml");
        extractFields();
    }

    private List<ProcessEventChange> extractFields() throws IOException, XMLStreamException {
        Issue issue = new Issue.Builder().projectPrefix("ASOC").id(48).build();
        InputStream inputStream = mockYouTrackInstance.getChangesUrl(issue).openStream();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);
        boolean extractingChange = false;
        String currentChangeType = "";
        String currentTag = "", currentFieldName = "";
        String oldValue = "", newValue = "";
        String updaterName = "";
        Date updated = null;
        List<ProcessEventChange> currentChanges = new ArrayList<>();

        while (eventReader.hasNext()) {
            XMLEvent nextEvent = eventReader.nextEvent();
            switch (nextEvent.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement startElement = nextEvent.asStartElement();
                    String elementName = startElement.getName().getLocalPart();
                    switch (elementName) {
                        case "change":
                            extractingChange = true;
                            currentTag = elementName;
                            break;
                        case "field":
                            currentFieldName = nextEvent.asStartElement().getAttributeByName(new QName("", "name")).getValue();
                            currentChangeType = nextEvent.asStartElement().getAttributes().next().toString();
                            log.info("found field named: {}: change type: {}", currentFieldName, currentChangeType);
                            break;
                        default:
//                            if (extractingChange) {
                                String elementText;
                                try {
                                    elementText = eventReader.getElementText();
                                    switch (elementName) {
                                        case "newValue":
                                            newValue = elementText;
                                            break;
                                        case "oldValue":
                                            oldValue = elementText;
                                            break;
                                        case "value":
                                            if (currentFieldName.equals("updaterName")) {
                                                updaterName = elementText;
                                            } else if (currentFieldName.equals("updated")) {
                                                updated = new Date(Long.parseLong(elementText));
                                            }
                                    }
                                } catch (Exception e) {
                                    //no text..
                                }

//                            }
                            break;
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    EndElement endElement = nextEvent.asEndElement();
                    String tagName = endElement.getName().getLocalPart();
                    if (tagName.equals("change")){
                        extractingChange = false;
                    } else if (tagName.equals("field")){
                        if (newValue.length() > 0) {
                            ProcessEventChange processEventChange = new ProcessEventChange.Builder()
                                    .updater(updaterName)
                                    .updated(updated)
                                    .field(currentFieldName)
                                    .priorValue(oldValue)
                                    .currentValue(newValue)
                                    .build();
                            currentChanges.add(processEventChange);
                            log.info("process event change: {}", processEventChange);
                        }
                        currentFieldName = ""; oldValue = ""; newValue = "";
                    }
                    break;

            }
        }
        log.info("returning changes: {}", currentChanges);
        return currentChanges;
    }

    private List<ProcessEventChange> buildProcessEventChanges() {
        return null;
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
