package edu.ucsb.cs156.frontiers.config;

import com.mongodb.client.MongoClient;
import de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"edu.ucsb.cs156.frontiers.mongo"})
@EnableAutoConfiguration(exclude = EmbeddedMongoAutoConfiguration.class)
public class MongoConfiguration {

  @Profile("development")
  @Configuration
  @Import(EmbeddedMongoAutoConfiguration.class)
  public class MongoLocalConfiguration {
    @Autowired private MongoClient mongoClient;

    @PostConstruct
    public void logPort() {
      System.out.println("Embedded Mongo running at: " + mongoClient.getClusterDescription());
    }
  }
}
