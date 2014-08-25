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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Create and schedule timer which will execute list of {@link EventListener}s
 * JobStarter.java
 */
public class JobStarter {
    private static Logger logger = LoggerFactory.getLogger(JobStarter.class);

    //TODO move to configuration params
    private static final long EXECUTION_DELAY = 2 * 1000;
    private static final long REPEAT_INTERVAL = 60 * 1000;

    private List<TimerTask> timerTasks;
    private Timer timer;

    public JobStarter() {
        initialize();
    }

    /**
     * Schedules periodic tasks to fetch the events
     */
    public void scheduleTasks() {
        final Configuration configuration = ConfigurationFactory.get();
        StreamProvider streamProvider = AuthenticatedHttpStreamProvider.basicAuthenticatedHttpStreamProvider(
                configuration.getString("PROP.YOUTRACK_USERNAME"), configuration.getString("PROP.YOUTRACK_PASSWORD")
        );

        scheduleTask(timer, new EventListenerImpl(streamProvider, new SlackInstance.Builder()
                .channelMapper(ChannelMapperFactory.fromConfiguration(configuration, "youtrack-slack.")).build()));
    }

    private void initialize() {
        timerTasks = new ArrayList<>(1);
        timer = new Timer();
    }

    /**
     * Schedules a periodic task {@link com.ontometrics.integrations.jobs.EventListener#checkForNewEvents()}
     * @param timer timer
     * @param eventListener event listener
     */
    private void scheduleTask(Timer timer, EventListener eventListener) {
        TimerTask timerTask = new EventTask(eventListener);
        timerTasks.add(timerTask);
        timer.schedule(timerTask, EXECUTION_DELAY, REPEAT_INTERVAL);
    }

    private static class EventTask extends TimerTask {
        private EventListener eventListener;

        private EventTask(EventListener eventListener) {
            this.eventListener = eventListener;
        }

        @Override
        public void run() {
            logger.info("Event processing started");
            try {
                this.eventListener.checkForNewEvents();
            } catch (ConfigurationAccessError error) {
                //this is critical error
                throw error;
            } catch (Throwable ex) {
                logger.error("Failed to process", ex);
            }
            logger.info("Event processing finished");
        }
    }



    public void dispose () {
        //cancelling all previously launched tasks and timer
        for (TimerTask timerTask : timerTasks) {
            timerTask.cancel();
        }
        timer.cancel();
        EventProcessorConfiguration.instance().dispose();
    }
}
