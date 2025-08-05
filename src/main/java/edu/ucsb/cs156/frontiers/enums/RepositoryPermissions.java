package edu.ucsb.cs156.frontiers.enums;

import lombok.Getter;

@Getter
public enum RepositoryPermissions {
    READ("pull"), WRITE("push"), MAINTAIN("maintain"), ADMIN("admin"),
    ;

    private final String apiName;

    private RepositoryPermissions(String apiName){
        this.apiName = apiName;
    }
}
