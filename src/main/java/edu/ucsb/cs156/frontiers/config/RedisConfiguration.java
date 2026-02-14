package edu.ucsb.cs156.frontiers.config;

import static org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig;
import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import io.lettuce.core.RedisURI;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import redis.embedded.RedisServer;

@EnableRedisRepositories(basePackages = {"edu.ucsb.cs156.frontiers.redis"})
@EnableCaching
@Configuration
@Slf4j
public class RedisConfiguration {

  @Value("${spring.data.redis.url}")
  private String redisUrl;

  @Configuration
  @Profile("!production")
  public class LocalStackConfiguration {

    private final RedisServer redisServer;
    private final RedisURI redisUri;

    public LocalStackConfiguration() throws IOException {
      this.redisUri = RedisURI.create(redisUrl);
      this.redisServer = new RedisServer(redisUri.getPort());
    }

    @PostConstruct
    public void postConstruct() throws IOException {
      redisServer.start();
      log.info("embedded Redis server started on port {}", redisUri.getPort());
    }

    @PreDestroy
    public void preDestroy() throws IOException {
      redisServer.stop();
      log.info("embedded Redis server stopped on port {}", redisUri.getPort());
    }
  }

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
      ObjectMapper mapper) {
    ObjectMapper redisMapper = mapper.copy();
    /* THIS SHOULD BE CHANGED WHEN WE MIGRATE TO SPRING BOOT 4 -- USE THE OPTION FOR NON_FINAL_AND_RECORDS
    SEE HERE: https://github.com/FasterXML/jackson-databind/issues/5223
    part of jackson 3.10*/
    redisMapper.activateDefaultTyping(
        redisMapper.getPolymorphicTypeValidator(), DefaultTyping.EVERYTHING, As.WRAPPER_ARRAY);
    GenericJackson2JsonRedisSerializer defaultSerializer =
        new GenericJackson2JsonRedisSerializer(redisMapper);
    return builder ->
        builder.cacheDefaults(
            defaultCacheConfig().serializeValuesWith(fromSerializer(defaultSerializer)));
  }
}
