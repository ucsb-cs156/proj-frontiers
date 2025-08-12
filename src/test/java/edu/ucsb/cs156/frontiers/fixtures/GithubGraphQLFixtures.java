package edu.ucsb.cs156.frontiers.fixtures;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class GithubGraphQLFixtures {

  public static final ObjectMapper objectMapper = new ObjectMapper();
  public static final String COMMITS_RESPONSE =
      """
      {
        "repository" : {
          "ref" : {
            "target" : {
              "history" : {
                "pageInfo" : {
                  "hasNextPage" : true,
                  "endCursor" : "79a22f53b228b10212b44f1a59c84a2999c23ecf 1"
                },
                "edges" : [ {
                  "node" : {
                    "oid" : "79a22f53b228b10212b44f1a59c84a2999c23ecf",
                    "url" : "https://github.com/ucsb-cs156/proj-frontiers/commit/79a22f53b228b10212b44f1a59c84a2999c23ecf",
                    "messageHeadline" : "Update azure-active-directory.md - formatting and clarification fixes",
                    "committedDate" : "2025-07-22T22:26:12Z",
                    "author" : {
                      "name" : "Phill Conrad",
                      "email" : "pconrad@cs.ucsb.edu",
                      "user" : {
                        "login" : "pconrad"
                      }
                    },
                    "committer" : {
                      "name" : "GitHub",
                      "email" : "noreply@github.com",
                      "user" : null
                    }
                  }
                }, {
                  "node" : {
                    "oid" : "13f8c8a8821b3f0636597f2d42d2359ad34183b5",
                    "url" : "https://github.com/ucsb-cs156/proj-frontiers/commit/13f8c8a8821b3f0636597f2d42d2359ad34183b5",
                    "messageHeadline" : "Merge pull request #228 from ucsb-cs156/dj-addAppropriateOrgStatus",
                    "committedDate" : "2025-07-22T22:04:48Z",
                    "author" : {
                      "name" : "Phill Conrad",
                      "email" : "pconrad@cs.ucsb.edu",
                      "user" : {
                        "login" : "pconrad"
                      }
                    },
                    "committer" : {
                      "name" : "GitHub",
                      "email" : "noreply@github.com",
                      "user" : null
                    }
                  }
                } ]
              }
            }
          }
        }
      }
      """;

  public static final Map<String, Object> COMMITS_RESPONSE_MAP;

  static {
    try {
      COMMITS_RESPONSE_MAP =
          objectMapper.readValue(COMMITS_RESPONSE, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
