package edu.ucsb.cs156.frontiers.models;

import com.opencsv.bean.AbstractBeanField;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * OpenCSV converter that joins a collection of team names into a single semicolon-delimited string
 * for CSV output. When the value is null or empty, an empty string is written.
 */
public class TeamsListToCsvConverter extends AbstractBeanField<Object, String> {

  @Override
  protected Object convert(String value) {
    // Reading is not used for downloads; return the raw value to satisfy abstract method.
    return value;
  }

  @Override
  protected String convertToWrite(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Collection<?>) {
      Collection<?> coll = (Collection<?>) value;
      if (coll.isEmpty()) {
        return "";
      }
      // Join non-null elements by semicolon with no spaces
      return coll.stream()
          .filter(Objects::nonNull)
          .map(Object::toString)
          .collect(Collectors.joining(";"));
    }
    // Fallback to toString() for unexpected types
    return value.toString();
  }
}
