package edu.ucsb.cs156.frontiers.enums;

import lombok.Getter;

@Getter
public enum DownloadRequestType {
  COMMITS("Commits");

  private String prettyName;

  DownloadRequestType(String prettyName) {
    this.prettyName = prettyName;
  }
}
