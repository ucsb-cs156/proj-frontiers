package edu.ucsb.cs156.frontiers.services;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import edu.ucsb.cs156.frontiers.models.PreConvertRosterStudent;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class RosterStudentService {

    private final UpdateUserService updateUserService;

    public enum InsertStatus {
        INSERTED, UPDATED, REJECTED
    }

    public record LoadResult(int inserted, int updated, List<RosterStudent> errors) {};
    public record UpsertResponse(InsertStatus insertStatus, RosterStudent rosterStudent){}

    private final RosterStudentRepository rosterStudentRepository;

    public RosterStudentService( RosterStudentRepository repository, UpdateUserService updateUserService) {
        this.rosterStudentRepository = repository;
        this.updateUserService = updateUserService;
    }


    /**
     * Loads and processes a CSV file containing student information for a specific course.
     * The CSV data is parsed into student objects, and an upsert operation is performed
     * for each student to either insert or update their details in the course roster.
     * In cases where the data is invalid or rejected, the related students are tracked as failures.
     *
     * @param file the CSV file containing student data
     * @param course the Course object specifying the course to which the students belong
     * @return a LoadResult object containing the count of inserted and updated students,
     *         as well as a list of students whose entries failed to process
     * @throws IOException if an error occurs while reading or processing the file
     */
    public LoadResult loadCsv(MultipartFile file, Course course) throws IOException {
        List<PreConvertRosterStudent> preConvertRosterStudents;
        List<RosterStudent> failures = new ArrayList<>();
        int[] counts = {0, 0};
        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            InputStreamReader reader = new InputStreamReader(inputStream);
            CsvToBean<PreConvertRosterStudent> csvToBean= new CsvToBeanBuilder<PreConvertRosterStudent>(reader)
                    .withType(PreConvertRosterStudent.class)
                    .withIgnoreEmptyLine(true)
                    .withFilter(line ->
                        Arrays.stream(line).anyMatch(s -> !s.isBlank())
                    )
                    .build();
            preConvertRosterStudents = new ArrayList<>(csvToBean.parse());
            preConvertRosterStudents.forEach(student -> {
                RosterStudent createdStudent = RosterStudent.builder()
                        .studentId(student.getStudentId())
                        .firstName(student.getFirstName())
                        .lastName(student.getLastName())
                        .email(student.getEmail())
                        .course(course)
                        .build();
                UpsertResponse response = upsertStudent(createdStudent, course, RosterStatus.ROSTER);
                if(response.insertStatus() == InsertStatus.REJECTED) {
                    failures.add(response.rosterStudent());
                } else {
                    counts[response.insertStatus.ordinal()]++;
                }
            });
            return new LoadResult(counts[0], counts[1], failures);
        }
    }


    /**
     * Performs an upsert operation for a given student into a course roster.
     * If a matching student exists based on their student ID or email, their information will be updated.
     * Otherwise, the student will be inserted as a new entry.
     * If there is a conflict between student ID and email pointing to different records, the operation is rejected.
     *
     * @param student the RosterStudent object containing student details to be upserted
     * @param course the Course object representing the course to which the student belongs
     * @param rosterStatus the RosterStatus indicating the status of the student's enrollment in the course
     * @return an UpsertResponse containing the status of the operation (INSERTED, UPDATED, or REJECTED)
     *         and the associated RosterStudent object
     */
    public UpsertResponse upsertStudent(RosterStudent student, Course course, RosterStatus rosterStatus) {
        String convertedEmail = CanonicalFormConverter.convertToValidEmail(student.getEmail());
        Optional<RosterStudent> existingStudent = rosterStudentRepository.findByCourseIdAndStudentId(course.getId(),
                student.getStudentId());
        Optional<RosterStudent> existingStudentByEmail = rosterStudentRepository.findByCourseIdAndEmail(course.getId(),
                convertedEmail);
        if(existingStudent.isPresent() && existingStudentByEmail.isPresent()) {
            if (!Objects.equals(existingStudent.get().getId(), existingStudentByEmail.get().getId())) {
                return new UpsertResponse(InsertStatus.REJECTED, student);
            } else {
                RosterStudent selectedStudent = existingStudent.get();
                selectedStudent.setRosterStatus(rosterStatus);
                selectedStudent.setFirstName(student.getFirstName());
                selectedStudent.setLastName(student.getLastName());
                rosterStudentRepository.save(selectedStudent);
                updateUserService.attachUserToRosterStudent(selectedStudent);
                return new UpsertResponse(InsertStatus.UPDATED, selectedStudent);
            }
        } else if (existingStudent.isPresent() || existingStudentByEmail.isPresent()) {
            RosterStudent existingStudentObj = existingStudent.orElseGet(existingStudentByEmail::get);
            existingStudentObj.setRosterStatus(rosterStatus);
            existingStudentObj.setFirstName(student.getFirstName());
            existingStudentObj.setLastName(student.getLastName());
            existingStudentObj.setEmail(convertedEmail);
            existingStudentObj.setStudentId(student.getStudentId());
            existingStudentObj = rosterStudentRepository.save(existingStudentObj);
            updateUserService.attachUserToRosterStudent(existingStudentObj);
            return new RosterStudentService.UpsertResponse(RosterStudentService.InsertStatus.UPDATED, existingStudentObj);
        } else {
            student.setRosterStatus(rosterStatus);
            //if an installationID exists, orgStatus should be set to JOINCOURSE. if it doesn't exist (null), set orgStatus to PENDING.
            if(course.getInstallationId() != null) {
                student.setOrgStatus(OrgStatus.JOINCOURSE);
            } else {
                student.setOrgStatus(OrgStatus.PENDING);
            }
            student = rosterStudentRepository.save(student);
            updateUserService.attachUserToRosterStudent(student);
            return new RosterStudentService.UpsertResponse(RosterStudentService.InsertStatus.INSERTED, student);
        }
    }

    /**
     * This method gets a list of RosterStudentDTOs based on the courseId
     * 
     * @param courseId if of the course
     * @return a list of RosterStudentDTOs
     */
    public List<RosterStudentDTO> getRosterStudentDTOs(Long courseId) {
        Iterable<RosterStudent> matchedStudents = rosterStudentRepository.findByCourseId(courseId);
        
        return StreamSupport.stream(matchedStudents.spliterator(), false)
            .map(RosterStudentDTO::new)
            .collect(Collectors.toList());

    }

    public StatefulBeanToCsv<RosterStudentDTO> getStatefulBeanToCSV(Writer writer) throws IOException {
        return new StatefulBeanToCsvBuilder<RosterStudentDTO>(writer).build();
    }

}
