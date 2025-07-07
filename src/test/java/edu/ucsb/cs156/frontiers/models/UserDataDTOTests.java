package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserDataDTOTests {

    @Test
    public void properly_translates_user_object_to_dto(){
        User user = User.builder()
                .id(1L)
                .email("cgaucho@ucsb.edu")
                .googleSub("fakesub")
                .pictureUrl("pictureurl")
                .fullName("Chris Gaucho")
                .givenName("Chris")
                .familyName("Gaucho")
                .build();

        UserDataDTO translated = UserDataDTO.builder()
                .id(1L)
                .email("cgaucho@ucsb.edu")
                .googleSub("fakesub")
                .pictureUrl("pictureurl")
                .fullName("Chris Gaucho")
                .givenName("Chris")
                .familyName("Gaucho")
                .admin(false)
                .instructor(false)
                .build();

        assertEquals(translated, UserDataDTO.from(user, false, false) );
    }

    @Test
    public void properly_translates_roles(){
        User user = User.builder()
                .id(1L)
                .email("cgaucho@ucsb.edu")
                .googleSub("fakesub")
                .pictureUrl("pictureurl")
                .fullName("Chris Gaucho")
                .givenName("Chris")
                .familyName("Gaucho")
                .build();

        UserDataDTO translated = UserDataDTO.builder()
                .id(1L)
                .email("cgaucho@ucsb.edu")
                .googleSub("fakesub")
                .pictureUrl("pictureurl")
                .fullName("Chris Gaucho")
                .givenName("Chris")
                .familyName("Gaucho")
                .admin(true)
                .instructor(true)
                .build();

        assertEquals(translated, UserDataDTO.from(user, true, true) );
    }
}
