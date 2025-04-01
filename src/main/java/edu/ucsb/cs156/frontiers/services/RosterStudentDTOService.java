package edu.ucsb.cs156.frontiers.services;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;

@Service
public class RosterStudentDTOService {


    @Autowired
    private RosterStudentRepository rosterStudentRepository;

    /**
     * This method gets a list of RosterStudentDTOs based on the courseId
     * 
     * @param courseId if of the course
     * @return a list of RosterStudentDTOs
     */
    public List<RosterStudentDTO> getRosterStudentDTOs(Long courseId) {
        Iterable<RosterStudent> matchedStudents = rosterStudentRepository.findByCourseId(courseId);
        
        return StreamSupport.stream(matchedStudents.spliterator(), false)
            .map(RosterStudentDTO::from)
            .collect(Collectors.toList());

    }

   
}
