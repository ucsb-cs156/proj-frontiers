package edu.ucsb.cs156.frontiers.dto;

import jakarta.validation.constraints.NotBlank;

public class RosterStudentUpdateDTO {

    @NotBlank
    private String studentId;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}