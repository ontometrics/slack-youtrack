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
 * This is pretty much a playground. Should probably delete this. Real tests are in 
 * {@link com.ontometrics.integrations.sources.EditSessionsExtractorTest}.
 * 
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
        mockYouTrackInstance = new MockIssueTracker.Builder()
                .feed("/feeds/issues-feed-rss.xml")
                .changes("/feeds/issue-changes.xml")
                .build();
        List<ProcessEventChange> changes = extractFields();
        mockYouTrackInstance = new MockIssueTracker.Builder()
                .feed("/feeds/issues-feed-rss.xml")
                .changes("/feeds/issue-changes2.xml")
                .build();
        assertThat(changes.size(), is(not(0)));
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
        private String filePathToFeed;
        private String filePathToChanges;
        private String filePathToAttachment;

        public MockIssueTracker(Builder builder) {
            filePathToFeed = builder.filePathToFeed;
            filePathToChanges = builder.filePathToChanges;
            filePathToAttachment = builder.filePathToAttachment;
        }

        public static class Builder {

            private String filePathToFeed;
            private String filePathToChanges;
            private String filePathToAttachment;

            public Builder feed(String filePathToFeed){
                this.filePathToFeed = filePathToFeed;
                return this;
                }

            public Builder changes(String filePathToChanges){
                this.filePathToChanges = filePathToChanges;
                return this;
                }

            public Builder attachments(String filePathToAttachment){
                this.filePathToAttachment = filePathToAttachment;
                return this;
                }

            public MockIssueTracker build(){
                return new MockIssueTracker(this);
                }
        }

        @Override
        public URL getBaseUrl() {
            return null;
        }

        @Override
        public URL getExternalBaseUrl() {
            return null;
        }

        @Override
        public URL getFeedUrl(String project, Date sinceDate) {
            return null;
        }

        @Override
        public URL getChangesUrl(Issue issue) {
            return TestUtil.getFileAsURL(filePathToChanges);
        }

        @Override
        public URL getAttachmentsUrl(Issue issue) {
            return TestUtil.getFileAsURL(filePathToAttachment);
        }

        @Override
        public String getIssueRestUrl(Issue issue) {
            return null;
        }

        @Override
        public URL getIssueUrl(String issueIdentifier) {
            return null;
        }

        @Override
        public URL getExternalIssueUrl(String issueIdentifier) {
            return null;
        }
    }

}
