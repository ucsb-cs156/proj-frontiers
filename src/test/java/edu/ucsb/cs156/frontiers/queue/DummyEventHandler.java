package edu.ucsb.cs156.frontiers.queue;

import edu.ucsb.cs156.frontiers.enums.EventType;

@EventHandler({EventType.REGISTER, EventType.LINK_GITHUB})
public class DummyEventHandler implements EventRunner{
    public void handleEvent(Event event) {
        // Do nothing
    }
}
