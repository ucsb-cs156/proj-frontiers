package edu.ucsb.cs156.frontiers.utilities;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.opencsv.bean.StatefulBeanToCsv;
import edu.ucsb.cs156.frontiers.models.CommitDto;
import java.io.StringWriter;
import java.io.Writer;
import org.junit.jupiter.api.Test;

public class StatefulBeanToCsvBuilderFactoryTests {

  @Test
  public void assert_statefulBeanToCsvBuilder_not_null() {
    StatefulBeanToCsvBuilderFactory factory = new StatefulBeanToCsvBuilderFactory();
    Writer writer = new StringWriter();
    StatefulBeanToCsv<CommitDto> csvWriter = factory.build(writer);
    assertNotNull(csvWriter);
  }
}
