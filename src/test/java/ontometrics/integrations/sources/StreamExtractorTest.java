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

    @Test
    public void testReadingExtraction() throws IOException, XMLStreamException {
        MockIssueTracker mockYouTrackInstance = new MockIssueTracker("/feeds/issues-feed-rss.xml", "/feeds/issue-changes.xml");

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

        List<FieldChange> fieldChanges = new ArrayList<>();
        while (eventReader.hasNext()) {
            XMLEvent nextEvent = eventReader.nextEvent();
            switch (nextEvent.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement startElement = nextEvent.asStartElement();
                    String elementName = startElement.getName().getLocalPart();
                    if (elementName.equals("change")) {
                        log.info("extracting change");
                        extractingChange = true;
                        currentTag = elementName;
                    } else if (elementName.equals("field")){
                        currentFieldName = nextEvent.asStartElement().getAttributeByName(new QName("", "name")).getValue();
                        currentChangeType = nextEvent.asStartElement().getAttributes().next().toString();
                        log.info("found field named: {}: change type: {}", currentFieldName, currentChangeType);
                    } else {
                        if (extractingChange){
                            String elementText = "";
                            try {
                                elementText = eventReader.getElementText();
                                if (elementName.equals("newValue")) {
                                    newValue = elementText;
                                } else if (elementName.equals("oldValue")){
                                    oldValue = elementText;
                                } else if (elementName.equals("value")){
                                    if (currentFieldName.equals("updaterName")){
                                        updaterName = elementText;
                                    } else if (currentFieldName.equals("updated")) {
                                        updated = new Date(Long.parseLong(elementText));
                                    }
                                }
                            } catch (Exception e){
                                //no text..
                            }

                            if (elementText.length() > 0) {
                                log.info("{}: {}", elementName, elementText);
                            } else {
                                log.info("found tag: {}", elementName);
                            }
                        }
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    EndElement endElement = nextEvent.asEndElement();
                    String tagName = endElement.getName().getLocalPart();
                    if (tagName.equals("change")){
                        log.info("got changes");
                        extractingChange = false;
                        buildChangeSet();
                    } else if (tagName.equals("field")){
                        if (newValue.length() > 0) {
                            ProcessEventChange processEventChange = new ProcessEventChange.Builder()
                                    .updater(updaterName)
                                    .updated(updated)
                                    .field(currentFieldName)
                                    .priorValue(oldValue)
                                    .currentValue(newValue)
                                    .build();
                            log.info("process event change: {}", processEventChange);
                        }
                        currentFieldName = ""; oldValue = ""; newValue = "";
                    }
                    break;

            }
        }

    }

    private void buildChangeSet() {
        List<ProcessEventChange> changes = buildProcessEventChanges();

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

    private static class FieldChange {
        private final String name;
        private final String oldValue;
        private final String newValue;

        public FieldChange(Builder builder) {
            name = builder.name;
            oldValue = builder.oldValue;
            newValue = builder.newValue;
        }

        public static class Builder {

            private String name;
            private String oldValue;
            private String newValue;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder oldValue(String value){
                this.oldValue = value;
                return this;
                }

            public Builder newValue(String newValue){
                this.newValue = newValue;
                return this;
                }

            public FieldChange build(){
                return new FieldChange(this);
                }
        }

        public String getName() {
            return name;
        }

        public String getOldValue() {
            return oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        @Override
        public String toString() {
            return "FieldChange{" +
                    "name='" + name + '\'' +
                    ", oldValue='" + oldValue + '\'' +
                    ", newValue='" + newValue + '\'' +
                    '}';
        }
    }


}


