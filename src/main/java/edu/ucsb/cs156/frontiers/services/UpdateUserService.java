package edu.ucsb.cs156.frontiers.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;

@Service
public class UpdateUserService {

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
        Iterable<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            attachRosterStudents(user);
        }
    }

    /**
     * This method attaches a SingleRoster student to the User based on their email.
     */
    public void attachUserToRosterStudent(RosterStudent rosterStudent) {
        String email = rosterStudent.getEmail();
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User matchedUser = optionalUser.get();
            rosterStudent.setUser(matchedUser);
            rosterStudentRepository.save(rosterStudent);
        }
    }
}
