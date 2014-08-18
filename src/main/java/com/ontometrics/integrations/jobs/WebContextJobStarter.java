package com.ontometrics.integrations.jobs;

import com.ontometrics.integrations.configuration.ConfigurationFactory;
import com.ontometrics.integrations.sources.ChannelMapper;
import com.ontometrics.integrations.sources.ExternalResourceInputStreamProvider;
import com.ontometrics.integrations.sources.InputStreamProvider;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Launch list of {@link com.ontometrics.integrations.jobs.EventListener} in a timer job on web-app startup
 * WebContextJobStarter.java
 */
public class WebContextJobStarter implements ServletContextListener {
    public static final String YT_FEED_URL = "http://ontometrics.com:8085/_rss/issues";
    private static Logger logger = LoggerFactory.getLogger(WebContextJobStarter.class);

    private static final long EXECUTION_DELAY = 2 * 1000;
    private static final long REPEAT_INTERVAL = 60 * 1000;

    private List<TimerTask> timerTasks;
    private Timer timer;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.info("Started up");
        initialize();
        scheduleTasks();
    }

    private void scheduleTasks() {
        try {
            Configuration configuration = ConfigurationFactory.get();
            InputStreamProvider inputStreamProvider = new ExternalResourceInputStreamProvider(
                    new URL(YT_FEED_URL).toExternalForm())
                .authenticator(httpExecutor -> httpExecutor.auth(configuration
                                    .getString("YOUTRACK_USERNAME"), configuration.getString("YOUTRACK_PASSWORD")));
            scheduleTask(timer, new EventListenerImpl(inputStreamProvider, createChannelMapper()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void initialize() {
        timerTasks = new ArrayList<>(3);
        timer = new Timer();
    }

    /**
     * Schedules a periodic task {@link EventListener#checkForNewEvents()}
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
            } catch (Throwable ex) {
                logger.error("Failed to process", ex);
            }
            logger.info("Event processing finished");
        }
    }

    private ChannelMapper createChannelMapper() {
        return new ChannelMapper.Builder()
                .defaultChannel("process")
                .addMapping("ASOC", "vixlet")
                .addMapping("HA", "jobspider")
                .addMapping("DMAN", "dminder")
                .build();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        for (TimerTask timerTask : timerTasks) {
            timerTask.cancel();
        }
        timer.cancel();
    }
}
