package edu.ucsb.cs156.frontiers.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.AccessLevel;

/**
 * This is a model class that represents system information.
 * 
 * This class is used to provide information about the system to the frontend.
 */

@Data
@AllArgsConstructor
@Builder
public class SystemInfo {
  private Boolean springH2ConsoleEnabled;
  private Boolean showSwaggerUILink;
  private String oauthLogin;
  private String sourceRepo; // user configured URL of the source repository for footer
  private String commitMessage;
  private String commitId;
  private String githubUrl; // URL to the commit in the source repository

 /** default constructor */
  public SystemInfo() {
  }

}
