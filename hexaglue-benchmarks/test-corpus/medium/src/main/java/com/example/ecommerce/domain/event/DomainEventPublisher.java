package com.example.ecommerce.domain.event;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory domain event publisher for testing purposes.
 */
public class DomainEventPublisher {
    private static final ThreadLocal<List<Object>> events = ThreadLocal.withInitial(ArrayList::new);

    public static void publish(Object event) {
        events.get().add(event);
    }

    public static List<Object> getEvents() {
        return new ArrayList<>(events.get());
    }

    public static void clear() {
        events.get().clear();
    }
}
