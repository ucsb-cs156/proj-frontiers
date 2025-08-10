package edu.ucsb.cs156.frontiers.queue;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.EventType;

public record Event (User user, EventType eventType) {}