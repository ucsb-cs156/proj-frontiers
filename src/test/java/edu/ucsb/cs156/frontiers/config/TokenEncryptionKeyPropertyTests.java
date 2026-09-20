package edu.ucsb.cs156.frontiers.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Checks how <code>application.properties</code> resolves the token encryption key: from <code>
 * TOKEN_ENCRYPTION_KEY</code>, falling back to the former name <code>
 * CANVAS_API_TOKEN_ENCRYPTION_KEY</code> so that existing deployments keep working.
 *
 * <p>The real system environment is deliberately left out, so that the result does not depend on
 * the machine the tests run on.
 */
public class TokenEncryptionKeyPropertyTests {

  private static final String PROPERTY = "app.canvas.api-token-encryption-key";

  private String resolve(Map<String, Object> environment) throws IOException {
    MutablePropertySources sources = new MutablePropertySources();
    sources.addLast(new MapPropertySource("environment", environment));
    sources.addLast(new ResourcePropertySource("classpath:application.properties"));
    return new PropertySourcesPropertyResolver(sources).getProperty(PROPERTY);
  }

  @Test
  void uses_TOKEN_ENCRYPTION_KEY() throws IOException {
    assertEquals("newKey", resolve(Map.of("TOKEN_ENCRYPTION_KEY", "newKey")));
  }

  @Test
  void uses_TOKEN_ENCRYPTION_KEY_from_dotenv() throws IOException {
    assertEquals("newKey", resolve(Map.of("env.TOKEN_ENCRYPTION_KEY", "newKey")));
  }

  @Test
  void falls_back_to_former_name() throws IOException {
    assertEquals("oldKey", resolve(Map.of("CANVAS_API_TOKEN_ENCRYPTION_KEY", "oldKey")));
    assertEquals("oldKey", resolve(Map.of("env.CANVAS_API_TOKEN_ENCRYPTION_KEY", "oldKey")));
  }

  @Test
  void new_name_wins_over_former_name() throws IOException {
    assertEquals(
        "newKey",
        resolve(
            Map.of("TOKEN_ENCRYPTION_KEY", "newKey", "CANVAS_API_TOKEN_ENCRYPTION_KEY", "oldKey")));
  }

  @Test
  void is_empty_when_neither_is_set() throws IOException {
    assertEquals("", resolve(Map.of()));
  }
}
