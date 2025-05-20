package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.controllers.ApiController;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.testconfig.TestConfig;

import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = DummyController.class)
@Import(TestConfig.class)
public class ApiControllerTests extends ControllerTestCase {

        @MockBean
        UserRepository userRepository;

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
        public void handleBadRequestException_returnsTypeAndMessage() {
                ApiController controller = new DummyController();

                BadRequestException ex = new BadRequestException(
                "Student number 'X' is already used in course 1"
                );

                // act
                Object raw = controller.handleBadRequestException(ex);

                // assert
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) raw;

                assertEquals("BadRequestException", result.get("type"));
                assertEquals(
                "Student number 'X' is already used in course 1",
                result.get("message")
                );
        }
    

}

