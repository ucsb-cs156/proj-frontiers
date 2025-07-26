package edu.ucsb.cs156.frontiers.queue;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Import( {EventManager.class})
public class EventManagerTests {
    @MockitoBean
    DummyEventHandler dummyEventHandler;

    @Autowired
    EventManager eventManager;

    @Test
    public void eventmanager_calls_events() {
        User user = User.builder().build();
        Event firedEvent = new Event(user, EventType.REGISTER);
        eventManager.fireEvent(firedEvent);
        verify(dummyEventHandler).handleEvent(eq(firedEvent));

        Event firedEvent2 = new Event(user, EventType.LINK_GITHUB);
        eventManager.fireEvent(firedEvent2);
        verify(dummyEventHandler).handleEvent(eq(firedEvent2));
    }

    @Test
    public void appropriately_filtered_handlers() {
        User user = User.builder().build();
        EventRunner noAnnotation = mock(EventRunner.class);
        EventManager withNoAnnotationMock = new EventManager(List.of(dummyEventHandler, noAnnotation));
        Event firedEvent = new Event(user, EventType.REGISTER);
        withNoAnnotationMock.fireEvent(firedEvent);
        verify(noAnnotation, never()).handleEvent(eq(firedEvent));
    }
}
