package edu.ucsb.cs156.frontiers.queue;

import edu.ucsb.cs156.frontiers.enums.EventType;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventManager {

    private final Map<EventType, List<EventRunner>> eventHandlerMap;

    public EventManager(List<EventRunner> eventRunners){
        this.eventHandlerMap = new HashMap<>();
        eventRunners.stream()
                .filter(handler -> handler.getClass().isAnnotationPresent(EventHandler.class))
                .forEach(handler -> {
                    EventType[] eventTypes = handler.getClass().getAnnotation(EventHandler.class).value();
                    for (EventType eventType : eventTypes) {
                        eventHandlerMap.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
                    }
                });

    }

    public void fireEvent(@NotNull Event event){
        eventHandlerMap.get(event.eventType()).forEach(handler -> handler.handleEvent(event));
    }
}
