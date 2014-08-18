package com.ontometrics.integrations.jobs;

import com.ontometrics.integrations.sources.ChannelMapper;
import org.apache.http.client.fluent.Executor;
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
    public static final String YT_USERNAME = "slackbot";
    public static final String YT_PASSWORD = "X9y-86A-bZN-93h";
    public static final String YT_FEED_URL = "http://ontometrics.com:8085/_rss/issues";
    private static Logger logger = LoggerFactory.getLogger(WebContextJobStarter.class);

    private static final long EXECUTION_DELAY = 2 * 1000;
    private static final long REPEAT_INTERVAL = 60 * 1000;

    private List<TimerTask> timerTasks;
    private Timer timer;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.info("Started up");
        timerTasks = new ArrayList<>(3);
        timer = new Timer();
        try {
            scheduleTask(timer, new EventListenerImpl(new URL(YT_FEED_URL),
                    createChannelMapper()).authenticator(httpExecutor -> httpExecutor.auth(YT_USERNAME, YT_PASSWORD)));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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
