package com.ontometrics.integrations.jobs;

import com.ontometrics.integrations.sources.ChannelMapper;

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
    private static final long EXECUTION_DELAY = 2 * 1000;
    private static final long REPEAT_INTERVAL = 60 * 1000;

    private List<TimerTask> timerTasks;
    private Timer timer;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        timerTasks = new ArrayList<>(3);
        timer = new Timer();
        try {
            scheduleTask(timer, new EventListenerImpl(new URL("http://ontometrics.com:8085/_rss/issues"), createChannelMapper()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        //TODO start timer here
    }

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
            this.eventListener.checkForNewEvents();
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
