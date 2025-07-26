package edu.ucsb.cs156.frontiers.queue.handlers;

import edu.ucsb.cs156.frontiers.enums.EventType;
import edu.ucsb.cs156.frontiers.queue.Event;
import edu.ucsb.cs156.frontiers.queue.EventHandler;
import edu.ucsb.cs156.frontiers.queue.EventRunner;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;

@EventHandler({EventType.LINK_GITHUB})
public class UpdateUserHandler implements EventRunner {
    private final UpdateUserService updateUserService;

    public UpdateUserHandler(UpdateUserService updateUserService) {
        this.updateUserService = updateUserService;
    }

    @Override
    public void handleEvent(Event event){
        updateUserService.attachCourseStaff(event.user());
        updateUserService.attachRosterStudents(event.user());
    }
}
