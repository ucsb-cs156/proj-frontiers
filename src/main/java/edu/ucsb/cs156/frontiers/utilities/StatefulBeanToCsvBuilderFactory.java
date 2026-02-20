package edu.ucsb.cs156.frontiers.utilities;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.bean.exceptionhandler.ExceptionHandlerIgnore;
import java.io.Writer;
import org.springframework.stereotype.Component;

@Component
public class StatefulBeanToCsvBuilderFactory {

  public <T> StatefulBeanToCsv<T> build(Class<T> type, Writer writer) {
    return new StatefulBeanToCsvBuilder<T>(writer)
        .withSeparator(',')
        .withQuotechar(CSVWriter.DEFAULT_QUOTE_CHARACTER)
        .withEscapechar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
        .withOrderedResults(true)
        .withExceptionHandler(new ExceptionHandlerIgnore())
        .build();
  }
}
