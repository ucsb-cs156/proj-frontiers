package edu.ucsb.cs156.frontiers.config;

import de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"edu.ucsb.cs156.frontiers.mongo.repositories"})
@EnableAutoConfiguration(exclude = EmbeddedMongoAutoConfiguration.class)
public class MongoConfiguration {

  @Profile("development")
  @Configuration
  @Import(EmbeddedMongoAutoConfiguration.class)
  public class MongoLocalConfiguration {
    //    public void mongoInstance(@Autowired MongoTemplate mongoTemplate) {}
  }
}
