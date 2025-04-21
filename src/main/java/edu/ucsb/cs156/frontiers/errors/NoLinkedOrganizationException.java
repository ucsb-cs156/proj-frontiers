package edu.ucsb.cs156.frontiers.errors;

public class NoLinkedOrganizationException extends RuntimeException{
    public NoLinkedOrganizationException (String courseName){
        super("No linked GitHub Organization to " + courseName + ". Please link a GitHub Organization first.");
    }
}
