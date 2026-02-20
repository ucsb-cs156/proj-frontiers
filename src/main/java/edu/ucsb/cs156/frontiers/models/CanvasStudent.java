package edu.ucsb.cs156.frontiers.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CanvasStudent {
  @JsonAlias({"firstName", "first_name"})
  private String firstName;

  private String lastName;
  private String email;
  private String studentId;

  @JsonCreator
  public CanvasStudent(
      @JsonProperty("sisId") String sisId, @JsonProperty("integrationId") String integrationId) {
    this.studentId = integrationId != null ? integrationId : sisId;
  }
}
