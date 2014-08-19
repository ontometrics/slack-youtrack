package ontometrics.test.util;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by rob on 8/19/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class ExtractorUtilTests {
    private Logger log = LoggerFactory.getLogger(ExtractorUtilTests.class);
    private URL changesUrl;
    private XMLEventReader reader;

    @Before
    public void setup() throws IOException, XMLStreamException {
        changesUrl = TestUtil.getFileAsURL("/feeds/issue-changes.xml");
        InputStream inputStream = changesUrl.openStream();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        reader = inputFactory.createXMLEventReader(inputStream);
    }

    @Test
    public void canExtractChangesAsNestedOperations() throws IOException, XMLStreamException {
        int elementCounter = 0;
        String currentElementName = "";
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.getEventType()== XMLStreamConstants.START_ELEMENT){
                currentElementName = event.asStartElement().getName().getLocalPart();
                if (currentElementName.equals("changes")){
                    extractChanges(event);
                }
            }

        }

    }

    private void extractChanges(XMLEvent event) {
        log.info("changes extraction on: {}", event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(buildIssueLink());
        stringBuilder.append(extractIndividualChanges());
    }

    private String extractIndividualChanges() {
        return "";
    }

    private String buildIssueLink() {
        return "";
    }

    @Test
    public void canExtractXSITypeFromElement(){

    }
}
