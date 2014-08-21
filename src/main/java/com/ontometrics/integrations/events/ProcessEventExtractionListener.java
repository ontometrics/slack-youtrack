package com.ontometrics.integrations.events;

import javax.xml.stream.events.XMLEvent;

/**
 * Created by rob on 8/21/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public interface ProcessEventExtractionListener {

    void onChangeSetFound(XMLEvent event);
    void onChange(XMLEvent event);

}
