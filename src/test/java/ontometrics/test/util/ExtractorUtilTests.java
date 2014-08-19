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

    @Before
    public void setup(){
        changesUrl = TestUtil.getFileAsURL("/feeds/issue-changes.xml");
    }

    @Test
    public void canExtractChangesAsNestedOperations() throws IOException, XMLStreamException {
        InputStream inputStream = changesUrl.openStream();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);

        int elementCounter = 0;
        String currentElementName = "";
        while (reader.hasNext()) {
            log.info("processing element: {}", elementCounter++);
            XMLEvent event = reader.nextEvent();
            if (event.getEventType()== XMLStreamConstants.START_ELEMENT){
                currentElementName = event.asStartElement().getName().getLocalPart();
                if (currentElementName.equals("change")){
                    extractChange(event);
                } else {
                    log.info("no handling for element type: {}", currentElementName);
                }
            }

        }

    }

    private void extractChange(XMLEvent event) {
        log.info("change extraction on: {}", event);
    }

    @Test
    public void canExtractXSITypeFromElement(){

    }
}
