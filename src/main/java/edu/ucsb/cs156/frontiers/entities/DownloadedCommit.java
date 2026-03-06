package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.opencsv.bean.CsvIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "downloaded_commit")
public class DownloadedCommit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @JoinColumn(
      name = "request_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "downloaded_commit_request_id_fk"))
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @CsvIgnore
  private DownloadRequest request;

  @JsonAlias("url")
  @NotNull
  private String commitUrl;
}
