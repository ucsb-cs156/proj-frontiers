package edu.ucsb.cs156.frontiers.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrgMember {

  @JsonProperty("id")
  private int githubId;

  @JsonProperty("login")
  private String githubLogin;
}
