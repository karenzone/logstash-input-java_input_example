package org.logstash.javaapi;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import co.elastic.logstash.api.v0.Input;
import org.apache.commons.lang3.StringUtils;
import org.logstash.execution.queue.QueueWriter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

// class name must match plugin name
@LogstashPlugin(name="java_input_example")
public class JavaInputExample implements Input {

    public static final PluginConfigSpec<Long> EVENT_COUNT_CONFIG =
            Configuration.numSetting("count", 3);

    public static final PluginConfigSpec<String> PREFIX_CONFIG =
            Configuration.stringSetting("prefix", "message");

    private long count;
    private String prefix;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped;

    // all plugins must provide a constructor that accepts Configuration and Context
    public JavaInputExample(Configuration config, Context context) {
        // constructors should validate configuration options
        count = config.get(EVENT_COUNT_CONFIG);
        prefix = config.get(PREFIX_CONFIG);
    }

    @Override
    public void start(QueueWriter queueWriter) {

        // The start method should push Map<String, Object> instances to the supplied QueueWriter
        // instance. Those will be converted to Event instances later in the Logstash event
        // processing pipeline.
        //
        // Inputs that operate on unbounded streams of data or that poll indefinitely for new
        // events should loop indefinitely until they receive a stop request. Inputs that produce
        // a finite sequence of events should loop until that sequence is exhausted or until they
        // receive a stop request, whichever comes first.

        int eventCount = 0;
        try {
            while (!stopped && eventCount < count) {
                eventCount++;
                queueWriter.push(Collections.singletonMap("message",
                        prefix + " " + StringUtils.center(eventCount + " of " + count, 20)));
            }
        } finally {
            stopped = true;
            done.countDown();
        }
    }

    @Override
    public void stop() {
        stopped = true; // set flag to request cooperative stop of input
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await(); // blocks until input has stopped
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Arrays.asList(EVENT_COUNT_CONFIG, PREFIX_CONFIG);
    }
}
