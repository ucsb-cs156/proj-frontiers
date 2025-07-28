package edu.ucsb.cs156.frontiers.services;

import com.google.common.collect.Lists;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.entities.Instructor;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.models.UserDataDTO;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDataDTOService {
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final InstructorRepository instructorRepository;

    @Autowired
    public UserDataDTOService(UserRepository userRepository, AdminRepository adminRepository, InstructorRepository instructorRepository){
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.instructorRepository = instructorRepository;
    }

    public Page<UserDataDTO> getUserDataDTOs(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        List<Admin> admins = Lists.newArrayList(adminRepository.findAll());
        List<Instructor> instructors = Lists.newArrayList(instructorRepository.findAll());

        List<UserDataDTO> userDTOs = new ArrayList<>();
        for(User user : users){
            boolean isAdmin = admins.stream().anyMatch(a -> a.getEmail().equals(user.getEmail()));
            boolean isInstructor = instructors.stream().anyMatch(i -> i.getEmail().equals(user.getEmail()));
            userDTOs.add(UserDataDTO.from(user, isAdmin, isInstructor));
        }
        return new PageImpl<UserDataDTO>(userDTOs, pageable, users.getTotalElements());
    }
}
