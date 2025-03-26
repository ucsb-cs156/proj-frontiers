package edu.ucsb.cs156.frontiers.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import edu.ucsb.cs156.frontiers.models.SystemInfo;

/**
 * This is a service for getting information about the system.
 * 
 * This class relies on property values. For hints on testing, see: <a href=
 * "https://www.baeldung.com/spring-boot-testing-configurationproperties">https://www.baeldung.com/spring-boot-testing-configurationproperties</a>
 * 
 */

@Slf4j
@Service("systemInfo")
@ConfigurationProperties
public class SystemInfoServiceImpl extends SystemInfoService {

  /** default constructor */
  public SystemInfoServiceImpl() {}

  @Value("${spring.h2.console.enabled:false}")
  private boolean springH2ConsoleEnabled;

  @Value("${app.showSwaggerUILink:false}")
  private boolean showSwaggerUILink;

  @Value("${app.oauth.login:/oauth2/authorization/google}")
  private String oauthLogin;

  @Value("${app.sourceRepo:https://github.com/ucsb-cs156/proj-courses}")
  private String sourceRepo;

  @Value("${git.commit.message.short:unknown}")
  private String commitMessage;

  @Value("${git.commit.id.abbrev:unknown}")
  private String commitId;

  /**
   * This method returns the github URL for the given repo and commit.
   * @param repo repository
   * @param commit commit SHA
   * @return the GitHub URL
   */
  public static String githubUrl(String repo, String commit) {
    return commit != null && repo != null ? repo + "/commit/" + commit : null;
  }

  /**
   * This method returns the system information.
   * 
   * @see edu.ucsb.cs156.frontiers.models.SystemInfo
   * @return the system information
   */
  public SystemInfo getSystemInfo() {
    SystemInfo si = SystemInfo.builder()
        .springH2ConsoleEnabled(this.springH2ConsoleEnabled)
        .showSwaggerUILink(this.showSwaggerUILink)
        .oauthLogin(this.oauthLogin)
        .sourceRepo(this.sourceRepo)
        .commitMessage(this.commitMessage)
        .commitId(this.commitId)
        .githubUrl(githubUrl(this.sourceRepo, this.commitId))
        .build();
    log.info("getSystemInfo returns {}", si);
    return si;
  }

}
