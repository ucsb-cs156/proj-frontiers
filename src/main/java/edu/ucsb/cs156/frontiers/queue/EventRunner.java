package edu.ucsb.cs156.frontiers.queue;

import edu.ucsb.cs156.frontiers.enums.EventType;

public interface EventRunner {
    void handleEvent(Event event);
}
