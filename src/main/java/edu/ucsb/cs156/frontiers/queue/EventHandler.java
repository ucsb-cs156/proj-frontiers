package edu.ucsb.cs156.frontiers.queue;

import edu.ucsb.cs156.frontiers.enums.EventType;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface EventHandler {
    EventType[] value();
}
