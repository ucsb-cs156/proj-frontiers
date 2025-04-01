package edu.ucsb.cs156.frontiers.models;


import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * This is a DTO class that represents a student in the roster.
 * It is used to transfer data between the server and the client.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RosterStudentDTO {
 
    private Long id;

    private Long courseId;
    
    private String studentId;
    private String firstName;
    private String lastName;
    private String email;

    private long userId;
    private int userGithubId;
    private String userGithubLogin;

    @Enumerated(EnumType.STRING)
    private RosterStatus rosterStatus;

    @Enumerated(EnumType.STRING)
    private OrgStatus orgStatus;


    public static RosterStudentDTO from(RosterStudent student) {
        long userId = student.getUser() != null ? student.getUser().getId() : 0;
        int userGithubId = student.getUser() != null ? student.getUser().getGithubId() : 0;
        String userGithubLogin = student.getUser() != null ? student.getUser().getGithubLogin() : "";
        
        return RosterStudentDTO.builder()
                .id(student.getId())
                .courseId(student.getCourse().getId())
                .studentId(student.getStudentId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .userId(userId)
                .userGithubId(userGithubId)
                .userGithubLogin(userGithubLogin)
                .rosterStatus(student.getRosterStatus())
                .orgStatus(student.getOrgStatus())
                .build();
    }
}
