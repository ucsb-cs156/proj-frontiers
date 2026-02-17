package edu.ucsb.cs156.frontiers.mongo.documents;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "temporary_commit_collection")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TemporaryCommitCollection {
  @Id private ObjectId id;

  @Indexed @Builder.Default private String sessionId = UUID.randomUUID().toString();

  private Long user;
}
