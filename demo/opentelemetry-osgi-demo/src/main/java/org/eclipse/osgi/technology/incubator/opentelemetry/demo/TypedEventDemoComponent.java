package org.eclipse.osgi.technology.incubator.opentelemetry.demo;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.typedevent.TypedEventBus;

/**
 * Demo component that periodically publishes typed events through the
 * OSGi Typed Event Bus to exercise the Typed Event integration.
 * Publishes events on various topics to generate a steady stream of
 * telemetry visible in Grafana.
 */
@Component(immediate = true)
public class TypedEventDemoComponent {

    private static final Logger LOG = Logger.getLogger(TypedEventDemoComponent.class.getName());
    private static final int INTERVAL_SECONDS = 8;
    private static final int INITIAL_DELAY_SECONDS = 15;

    private static final String[] TOPICS = {
        "org/eclipse/osgi/demo/heartbeat",
        "org/eclipse/osgi/demo/sensor/temperature",
        "org/eclipse/osgi/demo/sensor/humidity",
        "org/eclipse/osgi/demo/order/created",
        "org/eclipse/osgi/demo/order/shipped",
        "org/eclipse/osgi/demo/notification"
    };

    @Reference
    private TypedEventBus eventBus;

    private ScheduledExecutorService scheduler;
    private final Random random = new Random();
    private long sequence;

    @Activate
    public void activate() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "typed-event-demo");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::publishEvents,
            INITIAL_DELAY_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);

        LOG.info("TypedEventDemoComponent activated — publishing events every "
            + INTERVAL_SECONDS + "s");
    }

    @Deactivate
    public void deactivate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        LOG.info("TypedEventDemoComponent deactivated");
    }

    private void publishEvents() {
        try {
            // Publish 2-4 events per cycle on random topics
            int count = 2 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                String topic = TOPICS[random.nextInt(TOPICS.length)];
                publishEvent(topic);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error publishing demo typed events", e);
        }
    }

    private void publishEvent(String topic) {
        sequence++;
        DemoEvent event = new DemoEvent();
        event.sequence = sequence;
        event.timestamp = Instant.now().toString();
        event.source = "TypedEventDemoComponent";

        if (topic.contains("sensor/temperature")) {
            event.message = "Temperature reading";
            event.value = 18.0 + random.nextDouble() * 12.0;
        } else if (topic.contains("sensor/humidity")) {
            event.message = "Humidity reading";
            event.value = 30.0 + random.nextDouble() * 50.0;
        } else if (topic.contains("order/created")) {
            event.message = "New order created";
            event.value = 10.0 + random.nextDouble() * 990.0;
        } else if (topic.contains("order/shipped")) {
            event.message = "Order shipped";
            event.value = sequence;
        } else if (topic.contains("notification")) {
            event.message = "Notification #" + sequence;
            event.value = 0;
        } else {
            event.message = "Heartbeat #" + sequence;
            event.value = sequence;
        }

        eventBus.deliver(topic, event);
        LOG.fine(() -> "Published event on topic " + topic + ": " + event.message);
    }
}
