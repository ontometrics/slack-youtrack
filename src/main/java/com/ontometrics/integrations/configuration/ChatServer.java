package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.ProcessEventChange;

import java.util.List;

/**
 * <p>
 * Provides a means of interfacing to the Chat Server that is going to be
 * the outbound channel we use to communicate with the team.
 * </p>
 * <p>
 * Mapping of messages to channels is done internally. The post function assumes that we
 * know where we want the message to go inside the various channels/rooms, and thus, who
 * will see it.
 * </p>
 * <p>
 * Created by Rob on 8/23/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public interface ChatServer {

    /**
     * Put a message about a change out to the chat server.
     *
     * @param change some transformation of one or more properties on a given @{@link com.ontometrics.integrations.events.Issue}
     */
    void post(ProcessEventChange change);

    /**
     * Provides a list of the Users that are members of our chat server team.
     *
     * @return the usernames that are known members right now
     */
    List<String> getUsers();
}
