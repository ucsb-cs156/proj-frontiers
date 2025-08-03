package edu.ucsb.cs156.frontiers.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreConvertRosterStudentTests {
    @Test
    public void perm_and_name_set_correctly(){
        PreConvertRosterStudent student = new PreConvertRosterStudent();
        student.setPerm("123456");
        student.setFullName("Chris Gaucho");
        assertEquals("123456", student.getStudentId());
        assertEquals("Gaucho", student.getLastName());
        assertEquals("Chris", student.getFirstName());
    }

    @Test
    public void catches_one_name_only(){
        PreConvertRosterStudent student = new PreConvertRosterStudent();
        student.setPerm("123456");
        student.setFullName("Chris");
        assertEquals("123456", student.getStudentId());
        assertEquals("", student.getLastName());
    }
}
