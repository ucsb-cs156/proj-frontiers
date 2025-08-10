package edu.ucsb.cs156.frontiers.queue.handlers;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.EventType;
import edu.ucsb.cs156.frontiers.queue.Event;
import edu.ucsb.cs156.frontiers.queue.EventHandler;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class UpdateUserHandlerTests {
    @Mock
    UpdateUserService updateUserService;

    @InjectMocks
    UpdateUserHandler updateUserHandler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testUpdateUserHandler() {
        assertTrue(Arrays.stream(updateUserHandler.getClass().getAnnotation(EventHandler.class).value()).anyMatch(event -> event == EventType.LINK_GITHUB));
        Event event = new Event(User.builder().build(), EventType.LINK_GITHUB);
        updateUserHandler.handleEvent(event);
        verify(updateUserService).attachRosterStudents(eq(event.user()));
        verify(updateUserService).attachCourseStaff(eq(event.user()));
        verifyNoMoreInteractions(updateUserService);
    }
}
