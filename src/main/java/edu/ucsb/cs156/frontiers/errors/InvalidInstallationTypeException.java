package edu.ucsb.cs156.frontiers.errors;

public class InvalidInstallationTypeException extends RuntimeException {
    public InvalidInstallationTypeException (String type){
        super("Invalid installation type: " + type + ". Frontiers can only be linked to organizations");
    }
}
