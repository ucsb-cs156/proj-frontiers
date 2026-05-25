package edu.ucsb.cs156.frontiers.services;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.models.CourseStaffDTO;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseStaffDTOService {

  @Autowired private CourseStaffRepository courseStaffRepository;

  /**
   * This method gets a list of CourseStaffDTOs based on the courseId
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
    return new StatefulBeanToCsvBuilder<CourseStaffDTO>(writer).build();
  }
}
