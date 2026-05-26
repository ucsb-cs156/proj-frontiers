package edu.ucsb.cs156.frontiers.services;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.models.CourseStaffDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseStaffDTOService {

  @Autowired private CourseStaffRepository courseStaffRepository;

  private static final List<String> CSV_COLUMN_ORDER =
      List.of(
          "id",
          "courseId",
          "userId",
          "firstName",
          "lastName",
          "email",
          "orgStatus",
          "githubId",
          "githubLogin",
          "role");

  /**
   * This method gets a list of CourseStaffDTOs based on the courseId.
   *
   * @param courseId id of the course
   * @return a list of CourseStaffDTOs
   */
  public List<CourseStaffDTO> getCourseStaffDTOs(Long courseId) {
    Iterable<CourseStaff> matchedStaff = courseStaffRepository.findByCourseId(courseId);

    return StreamSupport.stream(matchedStaff.spliterator(), false)
        .map(CourseStaffDTO::new)
        .collect(Collectors.toList());
  }

  public StatefulBeanToCsv<CourseStaffDTO> getStatefulBeanToCSV(Writer writer) throws IOException {
    HeaderColumnNameMappingStrategy<CourseStaffDTO> strategy =
        new HeaderColumnNameMappingStrategy<>();
    strategy.setType(CourseStaffDTO.class);

    Map<String, Integer> positionByColumn =
        StreamSupport.stream(CSV_COLUMN_ORDER.spliterator(), false)
            .collect(
                Collectors.toMap(
                    column -> column.toUpperCase(Locale.US), CSV_COLUMN_ORDER::indexOf));

    Comparator<String> headerOrder =
        Comparator.comparingInt(
            column ->
                positionByColumn.getOrDefault(column.toUpperCase(Locale.US), Integer.MAX_VALUE));
    strategy.setColumnOrderOnWrite(headerOrder);

    return new StatefulBeanToCsvBuilder<CourseStaffDTO>(writer)
        .withMappingStrategy(strategy)
        .build();
  }
}
