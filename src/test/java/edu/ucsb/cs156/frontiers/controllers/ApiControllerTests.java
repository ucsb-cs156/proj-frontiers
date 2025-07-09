package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.controllers.ApiController;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;
import lombok.With;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(controllers = DummyController.class)
public class ApiControllerTests extends ControllerTestCase {

        @MockitoBean
        UserRepository userRepository;

        @Autowired DummyController dummyController;


        @Test
        public void generic_message_test() {
                ApiController apiController = new DummyController();
                Object result = apiController.genericMessage("Hello World");
                Object expected = Map.of("message", "Hello World");
                assertEquals(expected,result);
        }       

        @Test
        public void test_that_dummy_controller_returns_String1_when_1_is_passed() throws Exception {

                // act
                MvcResult response = mockMvc.perform(get("/dummycontroller?id=1"))
                                .andExpect(status().isOk()).andReturn();

                // assert

                assertEquals("String1", response.getResponse().getContentAsString());
        }

        @Test
        public void test_that_dummy_controller_returns_Exception_when_1_is_not_passed() throws Exception {

                // act
                MvcResult response = mockMvc.perform(get("/dummycontroller?id=7"))
                                .andExpect(status().isNotFound()).andReturn();

                // assert

                Map<String, Object> json = responseToJson(response);
                assertEquals("EntityNotFoundException", json.get("type"));
                assertEquals("String with id 7 not found", json.get("message"));
        }

        @Test
        public void test_dummy_controller_returns_no_linked_org() throws Exception {
                // act
                MvcResult response = mockMvc.perform(get("/dummycontroller/noorg?courseName=course"))
                        .andExpect(status().isBadRequest()).andReturn();

                // assert

                Map<String, Object> json = responseToJson(response);
                assertEquals("NoLinkedOrganizationException", json.get("type"));
                assertEquals("No linked GitHub Organization to course. Please link a GitHub Organization first.", json.get("message"));
        }

        @Test
        @WithMockUser(roles = { "ADMIN" })
        public void test_doesCurrentUserHaveRole_true() throws Exception {           
                assertTrue( dummyController.doesCurrentUserHaveRole("ROLE_ADMIN"));
                assertTrue( dummyController.isCurrentUserAdmin());

        }

         @Test
        @WithMockUser(roles = { "USER" })
        public void test_doesCurrentUserHaveRole_false() throws Exception {
                assertFalse( dummyController.doesCurrentUserHaveRole("ROLE_ADMIN"));
                assertFalse( dummyController.isCurrentUserAdmin());
        }

}

