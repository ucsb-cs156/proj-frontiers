package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.ucsb.cs156.frontiers.controllers.CoursesController.StudentCourseView;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;

public class StudentCourseViewTests {

    private static final String STUDENT_EMAIL = "student@ucsb.edu";

    private Course makeCourseWith(RosterStudent... students) {
        Course c = Course.builder()
                .id(1L)
                .installationId("inst1")
                .orgName("org-1")
                .courseName("Intro")
                .term("F25")
                .school("UCSB")
                .build();
        c.setRosterStudents(List.of(students));
        return c;
    }

    @Test
    public void status_whenNoRosterEntry() {
        RosterStudent other = new RosterStudent();
        other.setEmail("other@ucsb.edu");
        other.setOrgStatus(OrgStatus.MEMBER);

        StudentCourseView view = new StudentCourseView(
            makeCourseWith(other),
            STUDENT_EMAIL
        );

        assertEquals(
            "Not yet requested an invitation",
            view.status()
        );
    }

    @Test
    public void status_for_NONE() {
        RosterStudent rs = new RosterStudent();
        rs.setEmail(STUDENT_EMAIL);
        rs.setOrgStatus(OrgStatus.NONE);

        StudentCourseView view = new StudentCourseView(
            makeCourseWith(rs),
            STUDENT_EMAIL
        );

        assertEquals(
            "Not yet requested an invitation",
            view.status()
        );
    }

    @Test
    public void status_for_INVITED() {
        RosterStudent rs = new RosterStudent();
        rs.setEmail(STUDENT_EMAIL);
        rs.setOrgStatus(OrgStatus.INVITED);

        StudentCourseView view = new StudentCourseView(
            makeCourseWith(rs),
            STUDENT_EMAIL
        );

        assertEquals(
            "Has requested an invitation but isn't yet a member",
            view.status()
        );
    }

    @Test
    public void status_for_MEMBER() {
        RosterStudent rs = new RosterStudent();
        rs.setEmail(STUDENT_EMAIL);
        rs.setOrgStatus(OrgStatus.MEMBER);

        StudentCourseView view = new StudentCourseView(
            makeCourseWith(rs),
            STUDENT_EMAIL
        );

        assertEquals(
            "Is a member of the org",
            view.status()
        );
    }

    @Test
    public void status_for_OWNER() {
        RosterStudent rs = new RosterStudent();
        rs.setEmail(STUDENT_EMAIL);
        rs.setOrgStatus(OrgStatus.OWNER);

        StudentCourseView view = new StudentCourseView(
            makeCourseWith(rs),
            STUDENT_EMAIL
        );

        assertEquals(
            "Is an admin in the org",
            view.status()
        );
    }

    @Test
    public void status_for_EXPIRED() {
        RosterStudent rs = new RosterStudent();
        rs.setEmail(STUDENT_EMAIL);
        rs.setOrgStatus(OrgStatus.EXPIRED);

        StudentCourseView view = new StudentCourseView(
            makeCourseWith(rs),
            STUDENT_EMAIL
        );

        assertEquals(
            "Invitation has expired",
            view.status()
        );
    }
}