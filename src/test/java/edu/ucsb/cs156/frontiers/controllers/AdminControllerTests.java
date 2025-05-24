package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.entities.Admin;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;


@TestPropertySource(properties = {
    "app.admin.emails=testadmin@ucsb.edu"
})
@WebMvcTest(controllers = AdminController.class)
@Import(TestConfig.class)
public class AdminControllerTests extends ControllerTestCase{
    @MockBean
    AdminRepository adminRepository;

    @MockBean
    UserRepository userRepository;

    @Autowired
    private AdminController adminController;

    @Test
    public void logged_out_users_cannot_get_all() throws Exception {
            mockMvc.perform(get("/api/admin/admins/all"))
                            .andExpect(status().is(403)); 
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_canot_get_all() throws Exception {
            mockMvc.perform(get("/api/admin/admins/all"))
                            .andExpect(status().is(403)); 
    }

    @WithMockUser(roles = { "ADMIN" })
    @Test
    public void admin_users_can_get_all() throws Exception {
            mockMvc.perform(get("/api/admin/admins/all"))
                            .andExpect(status().is(200)); 
    }


        @Test
        public void logged_out_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/admin/admins/post"))
                                .andExpect(status().is(403));
        }

    

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_regular_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/admin/admins/post"))
                                .andExpect(status().is(403));
        }



     @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void admin_user_can_get_all() throws Exception {

                Admin admin = Admin.builder()
                    .email("jasonz2005@gmail.com")
                    .build();


                ArrayList<Admin> expectedAdmin = new ArrayList<>();
                expectedAdmin.add(admin);

                when(adminRepository.findAll(Sort.by("email"))).thenReturn(expectedAdmin);

                MvcResult response = mockMvc.perform(get("/api/admin/admins/all"))
                                .andExpect(status().isOk()).andReturn();


                verify(adminRepository, times(1)).findAll(Sort.by("email"));
                String expectedJson = mapper.writeValueAsString(expectedAdmin);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void an_admin_user_can_post_a_new_admin() throws Exception {
            
              Admin admin = Admin.builder()
                    .email("jasonz2005@gmail.com")
                    .build();

                when(adminRepository.save(eq(admin))).thenReturn(admin);

                MvcResult response = mockMvc.perform(
                                post("/api/admin/admins/post?email=jasonz2005@gmail.com")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                verify(adminRepository, times(1)).save(admin);
                String expectedJson = mapper.writeValueAsString(admin);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }
     

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_can_delete_a_admin() throws Exception {

                Admin admin = Admin.builder()
                    .email("jasonz2005@gmail.com")
                    .build();

                when(adminRepository.findById(eq("jasonz2005@gmail.com"))).thenReturn(Optional.of(admin));


                MvcResult response = mockMvc.perform(
                                delete("/api/admin/admins?email=jasonz2005@gmail.com")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                verify(adminRepository, times(1)).findById("jasonz2005@gmail.com");
                verify(adminRepository, times(1)).delete(any());

                Map<String, Object> json = responseToJson(response);
                assertEquals("Admin with email is deleted", json.get("message"));
        }
        

        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_tries_to_delete_non_existant_admin_and_gets_right_error_message() throws Exception {

                when(adminRepository.findById(eq("jasonz2005@gmail.com"))).thenReturn(Optional.empty());

                MvcResult response = mockMvc.perform(
                                delete("/api/admin/admins?email=jasonz2005@gmail.com")
                                                .with(csrf()))
                                .andExpect(status().isNotFound()).andReturn();

                verify(adminRepository, times(1)).findById("jasonz2005@gmail.com");
                Map<String, Object> json = responseToJson(response);
                assertEquals("Admin with id jasonz2005@gmail.com not found", json.get("message"));
        }


        @WithMockUser(roles = { "ADMIN" })
        @Test
        public void admin_cannot_delete_protected_admin_email() throws Exception {
            String protectedEmail = "protected@ucsb.edu";
    
            ReflectionTestUtils.setField(adminController, "protectedAdminEmails", new String[]{ protectedEmail });

            MvcResult response = mockMvc.perform(
                    delete("/api/admin/admins?email=protected@ucsb.edu")
                            .with(csrf()))
                    .andExpect(status().isForbidden()) 
                    .andReturn();

            verify(adminRepository, times(0)).delete(any());

            Map<String, Object> json = responseToJson(response);
            assertTrue(((String) json.get("message")).contains("Cannot delete protected admin: " + protectedEmail));
        }
}