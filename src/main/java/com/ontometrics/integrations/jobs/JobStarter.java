package com.ontometrics.integrations.jobs;

import com.ontometrics.integrations.configuration.ConfigurationAccessError;
import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.configuration.EventProcessorConfiguration;
import com.ontometrics.integrations.configuration.SlackInstance;
import com.ontometrics.integrations.sources.AuthenticatedHttpStreamProvider;
import com.ontometrics.integrations.sources.ChannelMapperFactory;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Create and schedule timer which will execute list of {@link EventListener}s
 * JobStarter.java
 */
public class JobStarter {
    private static final String CREDENTIALS_AUTH_TYPE = "credentials";
    private static Logger logger = LoggerFactory.getLogger(JobStarter.class);

    //TODO move to configuration params
    private static final long EXECUTION_DELAY = 2 * 1000;
    private static final long REPEAT_INTERVAL = 90 * 1000;
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture scheduledTask;

    public JobStarter() {
        initialize();
    }

    /**
     * Schedules periodic tasks to fetch the events
     */
    public void scheduleTasks() {
        final Configuration configuration = ConfigurationFactory.get();
        StreamProvider streamProvider = createStreamProvider(configuration);

        scheduleTask(new EventListenerImpl(streamProvider, new SlackInstance.Builder()
                .channelMapper(ChannelMapperFactory.fromConfiguration(configuration, "youtrack-slack.")).build()));
    }

    private StreamProvider createStreamProvider(Configuration configuration) {
        if (configuration.getString("PROP.AUTH_TYPE", CREDENTIALS_AUTH_TYPE).equalsIgnoreCase(CREDENTIALS_AUTH_TYPE)) {
            return AuthenticatedHttpStreamProvider.basicAuthenticatedHttpStreamProvider(
                    configuration.getString("PROP.YOUTRACK_USERNAME"), configuration.getString("PROP.YOUTRACK_PASSWORD")
            );
        }

        return AuthenticatedHttpStreamProvider.hubAuthenticatedHttpStreamProvider(
                configuration.getString("PROP.HUB_OAUTH_CLIENT_SERVICE_ID"),
                configuration.getString("PROP.HUB_OAUTH_CLIENT_SERVICE_SECRET"),
                configuration.getString("PROP.HUB_OAUTH_RESOURCE_SERVER_SERVICE_ID"),
                configuration.getString("PROP.HUB_URL")
        );
    }

    private void initialize() {
    }

    /**
     * Schedules a periodic task {@link com.ontometrics.integrations.jobs.EventListener#checkForNewEvents()}
     * @param eventListener event listener
     */
    private void scheduleTask(EventListener eventListener) {
        logger.info("Scheduling EventListener task");
        EventTask eventTask = new EventTask(eventListener);
        scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledTask = scheduledExecutorService
                .scheduleWithFixedDelay(eventTask, EXECUTION_DELAY, REPEAT_INTERVAL,
                        TimeUnit.MILLISECONDS);
    }

    private static class EventTask implements Runnable {
        private EventListener eventListener;

        private EventTask(EventListener eventListener) {
            this.eventListener = eventListener;
        }

        @Override
        public void run() {
            logger.info("Event processing started");
            try {
                this.eventListener.checkForNewEvents();
                logger.info("Event processing finished");
            } catch (ConfigurationAccessError error) {
                //this is critical error
                throw error;
            } catch (Throwable ex) {
                logger.error("Failed to process", ex);
            }
        }
    }

    public void dispose () {
        //cancelling all previously launched tasks and timer
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }

        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
        EventProcessorConfiguration.instance().dispose();
    }
}
