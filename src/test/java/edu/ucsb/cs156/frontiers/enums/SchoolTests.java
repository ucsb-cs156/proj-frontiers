package edu.ucsb.cs156.frontiers.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
public class SchoolTests {

  @Autowired private ObjectMapper mapper;

  @Test
  public void proper_serialization() throws Exception {
    School ucsb = mapper.convertValue("ucsb", School.class);
    assertEquals(School.UCSB, ucsb);

    School handlesNull = School.fromKey(null);
    assertNull(handlesNull);
    assertThrows(
        IllegalArgumentException.class, () -> mapper.convertValue("invalid", School.class));
    // language=JSON
    JsonNode node = mapper.readTree("""
        {
          "key": "UCSB"
        }
        """);
    School handlesAsKey = mapper.convertValue(node, School.class);
    assertEquals(School.UCSB, handlesAsKey);
    JsonNode nextTest =
        mapper.readTree("""
        {
          "noKeyField": "NotASchool"
        }
        """);
    assertThrows(IllegalArgumentException.class, () -> mapper.convertValue(nextTest, School.class));
  }
}
