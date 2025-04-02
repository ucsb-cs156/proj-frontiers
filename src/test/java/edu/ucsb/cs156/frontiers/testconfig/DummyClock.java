package edu.ucsb.cs156.frontiers.testconfig;

import io.jsonwebtoken.Clock;

import java.time.Instant;
import java.util.Date;

public class DummyClock implements Clock {
    @Override
    public Date now() {
        return Date.from(Instant.parse("2024-05-23T08:00:00.00Z"));
    }
}
