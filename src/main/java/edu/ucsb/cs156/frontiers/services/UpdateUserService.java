package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpdateUserService  {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RosterStudentRepository rosterStudentRepository;

    /**
     * This method attaches the RosterStudents to the User based on their email.
     * 
     * @param user The user to whom the RosterStudents will be attached
     */
    public void attachRosterStudents(User user) {
        List<RosterStudent> matchedStudents = rosterStudentRepository.findAllByEmail(user.getEmail());
        for (int i = 0; i < matchedStudents.size(); i++) {
            RosterStudent matchedStudent = matchedStudents.get(i);
            matchedStudent.setUser(user);
        }
        rosterStudentRepository.saveAll(matchedStudents);

    }

    /** attach roster students for all Users */
    public void attachRosterStudentsAllUsers() {
        // This method can be used to attach roster students for all users in the system
        Iterable<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            attachRosterStudents(user);
        }
    }
}
