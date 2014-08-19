package com.ontometrics.integrations.events;

import com.ontometrics.integrations.events.ProcessEvent;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * Created by rob on 7/17/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class EventDispatcher {

    public void postEvent(ProcessEvent event, String channel){
        Client client = ClientBuilder.newClient();
    }

}
