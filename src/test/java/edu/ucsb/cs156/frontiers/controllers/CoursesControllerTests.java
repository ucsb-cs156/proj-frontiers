package edu.ucsb.cs156.frontiers.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.ucsb.cs156.frontiers.ControllerTestCase;
import edu.ucsb.cs156.frontiers.annotations.WithInstructorCoursePermissions;
import edu.ucsb.cs156.frontiers.controllers.CoursesController.InstructorCourseView;
import edu.ucsb.cs156.frontiers.controllers.CoursesController.StaffCoursesDTO;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.errors.InvalidInstallationTypeException;
import edu.ucsb.cs156.frontiers.models.CourseWarning;
import edu.ucsb.cs156.frontiers.models.RosterStudentDTO;
import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.OrganizationLinkerService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@WebMvcTest(controllers = CoursesController.class)
public class CoursesControllerTests extends ControllerTestCase {

  @MockitoBean private CourseRepository courseRepository;

  @Autowired private CurrentUserService currentUserService;

  @MockitoBean private OrganizationLinkerService linkerService;

  @MockitoBean private UserRepository userRepository;

  @MockitoBean private RosterStudentRepository rosterStudentRepository;

  @MockitoBean private CourseStaffRepository courseStaffRepository;

  @MockitoBean private InstructorRepository instructorRepository;

  @MockitoBean private AdminRepository adminRepository;

  /** Test that ROLE_ADMIN can create a course */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testPostCourse_byAdmin() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .canvasApiToken("canvas-token")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(course);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/post")
                    .with(csrf())
                    .param("courseName", "CS156")
                    .param("term", "S25")
                    .param("school", "UCSB")
                    .param("canvasApiToken", "canvas-token")
                    .param("canvasCourseId", "12345"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).save(eq(course));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(course));
    assertEquals(expectedJson, responseString);
  }

  /** Test that ROLE_INSTRUCTOR can create a course */
  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void testPostCourse_byInstructor() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .canvasApiToken("canvas-token")
            .canvasCourseId("12345")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(course);

    // act

    MvcResult response =
        mockMvc
            .perform(
                post("/api/courses/post")
                    .with(csrf())
                    .param("courseName", "CS156")
                    .param("term", "S25")
                    .param("school", "UCSB")
                    .param("canvasApiToken", "canvas-token")
                    .param("canvasCourseId", "12345"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(courseRepository, times(1)).save(eq(course));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(course));
    assertEquals(expectedJson, responseString);
  }

  /** Test the GET all endpoint for courses for admins */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testAllCourses_ROLE_ADMIN() throws Exception {

    // arrange
    Course course1 = Course.builder().courseName("CS156").term("S25").school("UCSB").build();

    InstructorCourseView courseView1 = new InstructorCourseView(course1);

    Course course2 = Course.builder().courseName("CS148").term("S25").school("UCSB").build();
    InstructorCourseView courseView2 = new InstructorCourseView(course2);

    when(courseRepository.findAll()).thenReturn(java.util.List.of(course1, course2));

    // act

    MvcResult response =
        mockMvc.perform(get("/api/courses/allForAdmins")).andExpect(status().isOk()).andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(java.util.List.of(courseView1, courseView2));
    assertEquals(expectedJson, responseString);
  }

  /** Test the GET endpoint for courses for instructors */
  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void testAllCourses_ROLE_INSTRUCTOR() throws Exception {

    User user = currentUserService.getCurrentUser().getUser();

    // arrange
    Course course1 =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    InstructorCourseView courseView1 = new InstructorCourseView(course1);

    when(courseRepository.findByInstructorEmail(eq(user.getEmail())))
        .thenReturn(java.util.List.of(course1));

    // act

    MvcResult response =
        mockMvc
            .perform(get("/api/courses/allForInstructors"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(java.util.List.of(courseView1));
    verify(courseRepository, times(1)).findByInstructorEmail(eq(user.getEmail()));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testRedirect() throws Exception {
    doReturn("https://github.com/app/app1").when(linkerService).getRedirectUrl();

    String expectedUrl = "https://github.com/app/app1/installations/new?state=1";
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/redirect").param("courseId", "1"))
            .andExpect(status().isMovedPermanently())
            .andReturn();

    String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);

    assertEquals(expectedUrl, responseUrl);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testLinkCourseSuccessfully() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    Course course1 =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .id(1L)
            .build();

    CourseStaff courseStaff1 = CourseStaff.builder().orgStatus(OrgStatus.PENDING).build();
    RosterStudent rs1 = RosterStudent.builder().orgStatus(OrgStatus.PENDING).build();

    course1.setCourseStaff(List.of(courseStaff1));
    course1.setRosterStudents(List.of(rs1));

    CourseStaff courseStaff1Updated = CourseStaff.builder().orgStatus(OrgStatus.JOINCOURSE).build();
    RosterStudent rs1Updated = RosterStudent.builder().orgStatus(OrgStatus.JOINCOURSE).build();
    Course course2 =
        Course.builder()
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .installationId("1234")
            .orgName("ucsb-cs156-s25")
            .id(1L)
            .build();

    course2.setCourseStaff(List.of(courseStaff1Updated));
    course2.setRosterStudents(List.of(rs1Updated));

    doReturn(Optional.of(course1)).when(courseRepository).findById(eq(1L));
    doReturn("ucsb-cs156-s25").when(linkerService).getOrgName("1234");
    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/link")
                    .param("installation_id", "1234")
                    .param("setup_action", "install")
                    .param("code", "abcdefg")
                    .param("state", "1"))
            .andExpect(status().isMovedPermanently())
            .andReturn();

    String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);
    verify(courseRepository, times(1)).save(eq(course2));
    assertEquals("/login/success", responseUrl);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testLinkCourseSuccessfullyProfessorCreator() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    Course course1 =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .courseStaff(List.of())
            .rosterStudents(List.of())
            .id(1L)
            .build();
    Course course2 =
        Course.builder()
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .installationId("1234")
            .orgName("ucsb-cs156-s25")
            .id(1L)
            .courseStaff(List.of())
            .rosterStudents(List.of())
            .build();

    doReturn(Optional.of(course1)).when(courseRepository).findById(eq(1L));
    doReturn("ucsb-cs156-s25").when(linkerService).getOrgName("1234");
    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/link")
                    .param("installation_id", "1234")
                    .param("setup_action", "install")
                    .param("code", "abcdefg")
                    .param("state", "1"))
            .andExpect(status().isMovedPermanently())
            .andReturn();

    String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);
    verify(courseRepository, times(1)).save(eq(course2));
    assertEquals("/login/success", responseUrl);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testNoPerms() throws Exception {

    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/link")
                    .param("setup_action", "request")
                    .param("code", "abcdefg")
                    .param("state", "1"))
            .andExpect(status().isMovedPermanently())
            .andReturn();

    String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);
    assertEquals("/courses/nopermissions", responseUrl);
  }

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void testNotCreator() throws Exception {
    User separateUser = User.builder().id(2L).email("separate@example.com").build();
    Course course1 =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(separateUser.getEmail())
            .id(1L)
            .build();

    doReturn(Optional.of(course1)).when(courseRepository).findById(eq(1L));
    doReturn("ucsb-cs156-s25").when(linkerService).getOrgName("1234");
    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/link")
                    .param("installation_id", "1234")
                    .param("setup_action", "install")
                    .param("code", "abcdefg")
                    .param("state", "1"))
            .andExpect(status().isForbidden())
            .andReturn();
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testCourseLinkSuccessWhenAdminNotCreator() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    Long userId = user.getId();
    User separateUser = User.builder().id(userId + 1L).email("separate@example.com").build();
    Course courseBefore =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(separateUser.getEmail())
            .courseStaff(List.of())
            .rosterStudents(List.of())
            .id(1L)
            .build();
    Course courseAfter =
        Course.builder()
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(separateUser.getEmail())
            .installationId("1234")
            .orgName("ucsb-cs156-s25")
            .courseStaff(List.of())
            .rosterStudents(List.of())
            .id(1L)
            .build();

    doReturn(Optional.of(courseBefore)).when(courseRepository).findById(eq(1L));
    doReturn("ucsb-cs156-s25").when(linkerService).getOrgName("1234");
    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/link")
                    .param("installation_id", "1234")
                    .param("setup_action", "install")
                    .param("code", "abcdefg")
                    .param("state", "1"))
            .andExpect(status().isMovedPermanently())
            .andReturn();

    String responseUrl = response.getResponse().getHeader(HttpHeaders.LOCATION);
    verify(courseRepository, times(1)).save(eq(courseAfter));
    assertEquals("/login/success", responseUrl);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testCourseLinkNotFound() throws Exception {

    doReturn(Optional.empty()).when(courseRepository).findById(eq(1L));
    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/link")
                    .param("installation_id", "1234")
                    .param("setup_action", "install")
                    .param("code", "abcdefg")
                    .param("state", "1"))
            .andExpect(status().isNotFound())
            .andReturn();
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testNotOrganization() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    Course course1 =
        Course.builder()
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .id(1L)
            .build();

    doThrow(new InvalidInstallationTypeException("User"))
        .when(linkerService)
        .getOrgName(eq("1234"));
    doReturn(Optional.of(course1)).when(courseRepository).findById(eq(1L));
    MvcResult response =
        mockMvc
            .perform(
                get("/api/courses/link")
                    .param("installation_id", "1234")
                    .param("setup_action", "install")
                    .param("code", "abcdefg")
                    .param("state", "1"))
            .andExpect(status().isBadRequest())
            .andReturn();

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type",
            "InvalidInstallationTypeException",
            "message",
            "Invalid installation type: User. Frontiers can only be linked to organizations");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void testListCoursesForCurrentUser() throws Exception {
    String email = "student@example.com";

    OAuth2User principal = Mockito.mock(OAuth2User.class);
    when(principal.getAttribute("email")).thenReturn(email);

    Authentication auth =
        new OAuth2AuthenticationToken(
            principal,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
            "test-client");

    User dbUser = User.builder().email(email).build();
    when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(dbUser));

    Course course =
        Course.builder()
            .id(55L)
            .installationId("inst-55")
            .orgName("Test Org")
            .courseName("Test Course")
            .term("S25")
            .school("Engineering")
            .build();

    RosterStudent rs = new RosterStudent();
    rs.setId(123L);
    rs.setCourse(course);
    rs.setEmail(email);
    rs.setOrgStatus(OrgStatus.MEMBER);

    when(rosterStudentRepository.findAllByEmail(eq(email))).thenReturn(List.of(rs));

    MvcResult result =
        mockMvc
            .perform(get("/api/courses/list").with(authentication(auth)))
            .andExpect(status().isOk())
            .andReturn();

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("id", course.getId());
    expected.put("installationId", course.getInstallationId());
    expected.put("orgName", course.getOrgName());
    expected.put("courseName", course.getCourseName());
    expected.put("term", course.getTerm());
    expected.put("school", course.getSchool());
    expected.put("studentStatus", new RosterStudentDTO(rs).orgStatus());
    expected.put("rosterStudentId", rs.getId());

    String expectedJson = mapper.writeValueAsString(List.of(expected));
    assertEquals(expectedJson, result.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void testStudenIsStaffInCourse() throws Exception {
    // arrange
    User currentUser = User.builder().id(123L).email("user@example.org").build();

    Course course1 =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .build();

    Course course2 =
        Course.builder()
            .id(2L)
            .courseName("CS24")
            .orgName("ucsb-cs24-s25")
            .term("S25")
            .school("UCSB")
            .build();

    CourseStaff cs1 =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email("user@example.org")
            .course(course1)
            .user(currentUser)
            .id(37L)
            .build();

    CourseStaff cs2 =
        CourseStaff.builder()
            .firstName("Chris")
            .lastName("Gaucho")
            .email("user@example.org")
            .course(course2)
            .user(currentUser)
            .id(42L)
            .build();

    StaffCoursesDTO staffCourse1 =
        new StaffCoursesDTO(
            course1.getId(),
            course1.getInstallationId(),
            course1.getOrgName(),
            course1.getCourseName(),
            course1.getTerm(),
            course1.getSchool(),
            cs1.getOrgStatus(),
            cs1.getId());

    StaffCoursesDTO staffCourse2 =
        new StaffCoursesDTO(
            course2.getId(),
            course2.getInstallationId(),
            course2.getOrgName(),
            course2.getCourseName(),
            course2.getTerm(),
            course2.getSchool(),
            cs2.getOrgStatus(),
            cs2.getId());

    when(courseStaffRepository.findAllByEmail("user@example.org")).thenReturn(List.of(cs1, cs2));

    when(courseRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(course1, course2));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/staffCourses").param("studentId", "123"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(List.of(staffCourse1, staffCourse2));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testGetCourseById() throws Exception {
    // arrange
    User user = currentUserService.getCurrentUser().getUser();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(user.getEmail())
            .build();

    CoursesController.InstructorCourseView courseView =
        new CoursesController.InstructorCourseView(course);

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/1")).andExpect(status().isOk()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(courseView);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testGetCourseById_courseDoesNotExist() throws Exception {

    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/1")).andExpect(status().isNotFound()).andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testGetCourseById_AdminCanGetCourseCreatedBySomeoneElse() throws Exception {
    // arrange
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();

    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(otherInstructorUser.getEmail())
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc.perform(get("/api/courses/1")).andExpect(status().isOk()).andReturn();
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_returnsCorrectOutput() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(otherInstructorUser.getEmail())
            .installationId("inst-1")
            .canvasApiToken("canvas-token-1234567890")
            .canvasCourseId("canvas-course-123")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "canvas-course-123",
            "canvasApiToken", "********************890");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void testGetCanvasInfo_courseDoesNotExist() throws Exception {

    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_obscuresCorrectlyForLessThanThreeCharacters() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(otherInstructorUser.getEmail())
            .installationId("inst-1")
            .canvasApiToken("12")
            .canvasCourseId("canvas-course-123")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "canvas-course-123",
            "canvasApiToken", "12");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_obscuresCorrectlyForNoChars() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(otherInstructorUser.getEmail())
            .installationId("inst-1")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "",
            "canvasApiToken", "");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_obscuresCorrectlyForThreeCharacters() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(otherInstructorUser.getEmail())
            .installationId("inst-1")
            .canvasApiToken("123")
            .canvasCourseId("canvas-course-123")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "canvas-course-123",
            "canvasApiToken", "123");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void getCanvasInfo_obscuresCorrectlyForFourCharacters() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    User otherInstructorUser =
        User.builder().id(user.getId() + 1L).email("not_" + user.getEmail()).build();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .orgName("ucsb-cs156-s25")
            .term("S25")
            .school("UCSB")
            .instructorEmail(otherInstructorUser.getEmail())
            .installationId("inst-1")
            .canvasApiToken("1234")
            .canvasCourseId("canvas-course-123")
            .build();

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/getCanvasInfo").param("courseId", "1"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "courseId", "1",
            "canvasCourseId", "canvas-course-123",
            "canvasApiToken", "*234");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_not_found_returns_not_found() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());
    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository, linkerService, rosterStudentRepository);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_success_returns_ok() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .rosterStudents(Collections.emptyList())
            .courseStaff(Collections.emptyList())
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    verify(linkerService).unenrollOrganization(eq(course));
    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).delete(eq(course));
    verifyNoMoreInteractions(courseRepository, linkerService, rosterStudentRepository);
    String expectedJson = mapper.writeValueAsString(Map.of("message", "Course with id 1 deleted"));
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_course_with_students_throws_illegal_argument() throws Exception {
    RosterStudent student = RosterStudent.builder().id(1L).build();
    Course course =
        Course.builder()
            .id(1L)
            .rosterStudents(List.of(student))
            .courseStaff(Collections.emptyList())
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository, linkerService, rosterStudentRepository);

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "IllegalArgumentException",
            "message", "Cannot delete course with students or staff");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_course_with_staff_throws_illegal_argument() throws Exception {
    CourseStaff staff = CourseStaff.builder().id(1L).build();
    Course course =
        Course.builder()
            .id(1L)
            .rosterStudents(Collections.emptyList())
            .courseStaff(List.of(staff))
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository, linkerService, rosterStudentRepository);

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "IllegalArgumentException",
            "message", "Cannot delete course with students or staff");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void delete_course_with_students_and_staff_throws_illegal_argument() throws Exception {
    RosterStudent student = RosterStudent.builder().id(1L).build();
    CourseStaff staff = CourseStaff.builder().id(1L).build();
    Course course =
        Course.builder()
            .id(1L)
            .rosterStudents(List.of(student))
            .courseStaff(List.of(staff))
            .build();
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));

    MvcResult response =
        mockMvc
            .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
            .andExpect(status().isBadRequest())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository, linkerService, rosterStudentRepository);

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "IllegalArgumentException",
            "message", "Cannot delete course with students or staff");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void delete_course_non_admin_returns_forbidden() throws Exception {
    mockMvc
        .perform(delete("/api/courses").param("courseId", "1").with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(courseRepository, linkerService, rosterStudentRepository);
  }

  /**
   * Test that when we try to update the instructor emaii, if the course does not exist, it returns
   * an appropriate error.
   */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_courseDoesNotExist() throws Exception {

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateInstructor")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("instructorEmail", "new-instructor@example.com"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(eq(1L));

    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    Map<String, String> actualMap =
        mapper.readValue(responseString, new TypeReference<Map<String, String>>() {});
    assertEquals(expectedMap, actualMap);
  }

  /** Test that ROLE_ADMIN can update instructor email */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_byAdmin_email_is_instructor() throws Exception {
    User admin = currentUserService.getCurrentUser().getUser();
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("old-instructor@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(instructorRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(true);
    when(adminRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(false);

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("new-instructor@example.com")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateInstructor")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("instructorEmail", "new-instructor@example.com"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(eq(1L));
    verify(instructorRepository, times(1)).existsByEmail(eq("new-instructor@example.com"));
    verify(courseRepository, times(1)).save(any(Course.class));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  /** Test that ROLE_ADMIN can update instructor email */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_byAdmin_email_is_admin() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("old-instructor@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(instructorRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(false);
    when(adminRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(true);

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("new-instructor@example.com")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateInstructor")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("instructorEmail", "new-instructor@example.com"))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(eq(1L));
    verify(instructorRepository, times(1)).existsByEmail(eq("new-instructor@example.com"));
    verify(courseRepository, times(1)).save(eq(updatedCourse));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  /** Test that updating an instructor email sanitizes the address properly */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_byAdmin_email_is_sanitized() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("old-instructor@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(instructorRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(false);
    when(adminRepository.existsByEmail(eq("new-instructor@example.com"))).thenReturn(true);

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("new-instructor@example.com")
            .build();

    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateInstructor")
                    .with(csrf())
                    .param("courseId", "1")
                    .param("instructorEmail", " new-instructor@example.com "))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(courseRepository, times(1)).findById(eq(1L));
    verify(instructorRepository, times(1)).existsByEmail(eq("new-instructor@example.com"));
    verify(courseRepository, times(1)).save(eq(updatedCourse));

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  /**
   * Test that updateInstructorEmail fails when email doesn't exist in instructor or admin tables
   */
  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testUpdateInstructorEmail_emailNotFound() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("old-instructor@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(instructorRepository.existsByEmail(eq("nonexistent@example.com"))).thenReturn(false);
    when(adminRepository.existsByEmail(eq("nonexistent@example.com"))).thenReturn(false);

    // act & assert
    mockMvc
        .perform(
            put("/api/courses/updateInstructor")
                .with(csrf())
                .param("courseId", "1")
                .param("instructorEmail", "nonexistent@example.com"))
        .andExpect(status().isBadRequest());

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(instructorRepository, times(1)).existsByEmail(eq("nonexistent@example.com"));
    verify(adminRepository, times(1)).existsByEmail(eq("nonexistent@example.com"));
    verify(courseRepository, never()).save(any(Course.class));
  }

  /** Test that updateInstructorEmail requires ADMIN role */
  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void testUpdateInstructorEmail_requiresAdmin() throws Exception {
    // act & assert
    mockMvc
        .perform(
            put("/api/courses/updateInstructor")
                .with(csrf())
                .param("courseId", "1")
                .param("instructorEmail", "new-instructor@example.com"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void updateCourse_success_admin() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("OldName")
            .term("OldTerm")
            .school("OldSchool")
            .instructorEmail("rando@example.com")
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("NewName")
            .term("NewTerm")
            .school("NewSchool")
            .instructorEmail("rando@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult result =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "1")
                    .param("courseName", "NewName")
                    .param("term", "NewTerm")
                    .param("school", "NewSchool")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(courseRepository, times(1)).save(any(Course.class));

    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, result.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void updateCourse_notFound() throws Exception {
    when(courseRepository.findById(eq(2L))).thenReturn(Optional.empty());

    MvcResult result =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "2")
                    .param("courseName", "AnyName")
                    .param("term", "AnyTerm")
                    .param("school", "AnySchool")
                    .with(csrf()))
            .andExpect(status().isForbidden())
            .andReturn();

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void updateCourse_forbidden_for_non_instructor() throws Exception {
    mockMvc
        .perform(
            put("/api/courses")
                .param("courseId", "1")
                .param("courseName", "NewName")
                .param("term", "NewTerm")
                .param("school", "NewSchool")
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void update_course_not_found_returns_not_found() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "1")
                    .param("courseName", "Updated Course")
                    .param("term", "F25")
                    .param("school", "Updated School")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository);
  }

  @Test
  @WithInstructorCoursePermissions
  public void update_course_success_returns_ok() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school("Original School")
            .instructorEmail(user.getEmail())
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Updated Course")
            .term("F25")
            .school("Updated School")
            .instructorEmail(user.getEmail())
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "1")
                    .param("courseName", "Updated Course")
                    .param("term", "F25")
                    .param("school", "Updated School")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).save(updatedCourse);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void admin_can_update_course_created_by_someone_else() throws Exception {
    User adminUser = currentUserService.getCurrentUser().getUser();
    User instructorUser =
        User.builder().id(adminUser.getId() + 1L).email("instructor@example.com").build();

    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school("Original School")
            .instructorEmail(instructorUser.getEmail())
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Admin Updated Course")
            .term("F25")
            .school("Admin Updated School")
            .instructorEmail(instructorUser.getEmail())
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses")
                    .param("courseId", "1")
                    .param("courseName", "Admin Updated Course")
                    .param("term", "F25")
                    .param("school", "Admin Updated School")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).save(updatedCourse);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void updateCourseCanvasToken_success_admin() throws Exception {

    Course course =
        Course.builder()
            .id(1L)
            .courseName("Name")
            .term("Term")
            .school("School")
            .instructorEmail("rando@example.com")
            .canvasApiToken("oldToken")
            .canvasCourseId("oldCourseId")
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Name")
            .term("Term")
            .school("School")
            .instructorEmail("rando@example.com")
            .canvasApiToken("newToken")
            .canvasCourseId("newCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult result =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "1")
                    .param("canvasApiToken", "newToken")
                    .param("canvasCourseId", "newCourseId")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository, times(1)).findById(eq(1L));
    verify(courseRepository, times(1)).save(any(Course.class));

    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, result.getResponse().getContentAsString());
  }

  @Test
  @WithMockUser(roles = {"INSTRUCTOR"})
  public void updateCourseCanvasToken_notFound() throws Exception {
    when(courseRepository.findById(eq(2L))).thenReturn(Optional.empty());

    MvcResult result =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "2")
                    .param("canvasApiToken", "AnyToken")
                    .param("canvasCourseId", "AnyCourseId")
                    .with(csrf()))
            .andExpect(status().isForbidden())
            .andReturn();

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  @WithMockUser(roles = {"USER"})
  public void updateCourseCanvasToken_forbidden_for_non_instructor() throws Exception {
    mockMvc
        .perform(
            put("/api/courses/updateCourseCanvasToken")
                .param("courseId", "1")
                .param("canvasApiToken", "newToken")
                .param("canvasCourseId", "newCourseId")
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateCourseCanvasToken_not_found_returns_not_found() throws Exception {
    when(courseRepository.findById(eq(1L))).thenReturn(Optional.empty());
    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "1")
                    .param("canvasApiToken", "newToken")
                    .param("canvasCourseId", "newCourseId")
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
    verify(courseRepository).findById(eq(1L));
    verifyNoMoreInteractions(courseRepository);
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateCourseCanvasToken_success_returns_ok() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();

    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school("Original School")
            .instructorEmail(user.getEmail())
            .canvasApiToken("originalToken")
            .canvasCourseId("originalCourseId")
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school("Original School")
            .instructorEmail(user.getEmail())
            .canvasApiToken("newToken")
            .canvasCourseId("newCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "1")
                    .param("canvasApiToken", "newToken")
                    .param("canvasCourseId", "newCourseId")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).save(updatedCourse);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void admin_can_updateCourseCanvasToken_created_by_someone_else() throws Exception {
    User adminUser = currentUserService.getCurrentUser().getUser();
    User instructorUser =
        User.builder().id(adminUser.getId() + 1L).email("instructor@example.com").build();

    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school("Original School")
            .instructorEmail(instructorUser.getEmail())
            .canvasApiToken("originalToken")
            .canvasCourseId("originalCourseId")
            .build();

    Course updatedCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school("Original School")
            .instructorEmail(instructorUser.getEmail())
            .canvasApiToken("newToken")
            .canvasCourseId("newCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/courses/updateCourseCanvasToken")
                    .param("courseId", "1")
                    .param("canvasApiToken", "newToken")
                    .param("canvasCourseId", "newCourseId")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(courseRepository).findById(eq(1L));
    verify(courseRepository).save(updatedCourse);

    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new InstructorCourseView(updatedCourse));
    assertEquals(expectedJson, responseString);
  }

  // Tests for InstructorCourseView constructor with null collections
  @Test
  public void testInstructorCourseView_withNullRosterStudents() throws Exception {
    /** Test that InstructorCourseView correctly counts students and staff */
    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("instructor@example.com")
            .installationId("123")
            .orgName("test-org")
            .rosterStudents(null) // explicitly null
            .courseStaff(List.of()) // empty list
            .build();

    // act
    InstructorCourseView view = new InstructorCourseView(course);

    // assert
    assertEquals(1L, view.id());
    assertEquals("CS156", view.courseName());
    assertEquals("S25", view.term());
    assertEquals("UCSB", view.school());
    assertEquals("instructor@example.com", view.instructorEmail());
    assertEquals("123", view.installationId());
    assertEquals("test-org", view.orgName());
    assertEquals(0, view.numStudents()); // should be 0 when null
    assertEquals(0, view.numStaff()); // should be 0 for empty list
  }

  @Test
  public void testInstructorCourseView_withNullCourseStaff() throws Exception {
    // arrange
    Course course =
        Course.builder()
            .id(2L)
            .courseName("CS148")
            .term("F25")
            .school("UCSB")
            .instructorEmail("instructor@example.com")
            .installationId("456")
            .orgName("test-org-2")
            .rosterStudents(List.of()) // empty list
            .courseStaff(null) // explicitly null
            .build();

    // act
    InstructorCourseView view = new InstructorCourseView(course);

    // assert
    assertEquals(2L, view.id());
    assertEquals("CS148", view.courseName());
    assertEquals("F25", view.term());
    assertEquals("UCSB", view.school());
    assertEquals("instructor@example.com", view.instructorEmail());
    assertEquals("456", view.installationId());
    assertEquals("test-org-2", view.orgName());
    assertEquals(0, view.numStudents()); // should be 0 for empty list
    assertEquals(0, view.numStaff()); // should be 0 when null
  }

  @Test
  public void testInstructorCourseView_withBothCollectionsNull() throws Exception {
    // arrange
    Course course =
        Course.builder()
            .id(3L)
            .courseName("CS24")
            .term("W25")
            .school("UCSB")
            .instructorEmail("instructor@example.com")
            .installationId("789")
            .orgName("test-org-3")
            .rosterStudents(null) // explicitly null
            .courseStaff(null) // explicitly null
            .build();

    // act
    InstructorCourseView view = new InstructorCourseView(course);

    // assert
    assertEquals(3L, view.id());
    assertEquals("CS24", view.courseName());
    assertEquals("W25", view.term());
    assertEquals("UCSB", view.school());
    assertEquals("instructor@example.com", view.instructorEmail());
    assertEquals("789", view.installationId());
    assertEquals("test-org-3", view.orgName());
    assertEquals(0, view.numStudents()); // should be 0 when null
    assertEquals(0, view.numStaff()); // should be 0 when null
  }

  @Test
  public void testInstructorCourseView_withNonNullCollections() throws Exception {
    // arrange
    RosterStudent student1 = RosterStudent.builder().id(1L).build();
    RosterStudent student2 = RosterStudent.builder().id(2L).build();
    CourseStaff staff1 = CourseStaff.builder().id(1L).build();
    CourseStaff staff2 = CourseStaff.builder().id(2L).build();
    CourseStaff staff3 = CourseStaff.builder().id(3L).build();

    Course course =
        Course.builder()
            .id(4L)
            .courseName("CS130A")
            .term("S25")
            .school("UCSB")
            .instructorEmail("instructor@example.com")
            .installationId("101112")
            .orgName("test-org-4")
            .rosterStudents(List.of(student1, student2)) // 2 students
            .courseStaff(List.of(staff1, staff2, staff3)) // 3 staff
            .build();

    // act
    InstructorCourseView view = new InstructorCourseView(course);

    // assert
    assertEquals("CS130A", view.courseName());
    assertEquals("S25", view.term());
    assertEquals("UCSB", view.school());
    assertEquals("instructor@example.com", view.instructorEmail());
    assertEquals("101112", view.installationId());
    assertEquals("test-org-4", view.orgName());
    assertEquals(2, view.numStudents()); // should match list size
    assertEquals(3, view.numStaff()); // should match list size
  }

  @Test
  public void testInstructorCourseView_countsStudentsAndStaff() {
    // arrange
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("test@example.com")
            .build();

    // Test with null lists
    InstructorCourseView viewWithNulls = new InstructorCourseView(course);
    assertEquals(0, viewWithNulls.numStudents());
    assertEquals(0, viewWithNulls.numStaff());

    // Test with empty lists
    course.setRosterStudents(Collections.emptyList());
    course.setCourseStaff(Collections.emptyList());
    InstructorCourseView viewWithEmpty = new InstructorCourseView(course);
    assertEquals(0, viewWithEmpty.numStudents());

    // Test with populated lists
    RosterStudent student1 = RosterStudent.builder().id(1L).build();
    RosterStudent student2 = RosterStudent.builder().id(2L).build();
    CourseStaff staff1 = CourseStaff.builder().id(1L).build();

    course.setRosterStudents(List.of(student1, student2));
    course.setCourseStaff(List.of(staff1));
    InstructorCourseView viewWithData = new InstructorCourseView(course);
    assertEquals(2, viewWithData.numStudents());
    assertEquals(1, viewWithData.numStaff());
  }

  @Test
  @WithInstructorCoursePermissions
  public void calls_org_service_for_warnings() throws Exception {
    Course course =
        Course.builder()
            .id(1L)
            .courseName("CS156")
            .term("S25")
            .school("UCSB")
            .instructorEmail("test@example.com")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(course));
    when(linkerService.checkCourseWarnings(eq(course))).thenReturn(new CourseWarning(true));

    MvcResult response =
        mockMvc.perform(get("/api/courses/warnings/1")).andExpect(status().isOk()).andReturn();

    verify(linkerService).checkCourseWarnings(eq(course));
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(new CourseWarning(true));
    assertEquals(expectedJson, responseString);
  }

  @Test
  @WithInstructorCoursePermissions
  public void test_warnings_not_found() throws Exception {

    doReturn(Optional.empty()).when(courseRepository).findById(eq(1L));
    MvcResult response =
        mockMvc
            .perform(get("/api/courses/warnings/1"))
            .andExpect(status().isNotFound())
            .andReturn();
    verify(linkerService, never()).checkCourseWarnings(any());
    String responseString = response.getResponse().getContentAsString();
    Map<String, String> expectedMap =
        Map.of(
            "type", "EntityNotFoundException",
            "message", "Course with id 1 not found");
    String expectedJson = mapper.writeValueAsString(expectedMap);
    assertEquals(expectedJson, responseString);
  }

  // @Test
  // @WithInstructorCoursePermissions
  // public void updateCourseCanvasToken_same_value_does_not_change() throws Exception {
  //     User user = currentUserService.getCurrentUser().getUser();

  //     Course originalCourse = Course.builder()
  //         .id(1L)
  //         .courseName("Original Course")
  //         .term("S25")
  //         .school("Original School")
  //         .instructorEmail(user.getEmail())
  //         .canvasApiToken("sameToken")
  //         .canvasCourseId("sameCourseId")
  //         .build();

  //     when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
  //     when(courseRepository.save(any(Course.class))).thenReturn(originalCourse);

  //     mockMvc.perform(put("/api/courses/updateCourseCanvasToken")
  //             .param("courseId", "1")
  //             .param("canvasApiToken", "sameToken")
  //             .param("canvasCourseId", "sameCourseId")
  //             .with(csrf()))
  //         .andExpect(status().isOk());

  //     verify(courseRepository).save(eq(originalCourse));
  //     assertEquals("sameToken", originalCourse.getCanvasApiToken());
  //     assertEquals("sameCourseId", originalCourse.getCanvasCourseId());
  // }
  @Test
  @WithInstructorCoursePermissions
  public void updateCourseCanvasToken_empty_string_no_change() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school("Original School")
            .instructorEmail(user.getEmail())
            .canvasApiToken("existingToken")
            .canvasCourseId("existingCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(originalCourse);

    mockMvc
        .perform(
            put("/api/courses/updateCourseCanvasToken")
                .param("courseId", "1")
                .param("canvasApiToken", "")
                .param("canvasCourseId", "")
                .with(csrf()))
        .andExpect(status().isOk());

    verify(courseRepository).save(originalCourse);
  }

  @Test
  @WithInstructorCoursePermissions
  public void updateCourseCanvasToken_null_params_no_change() throws Exception {
    User user = currentUserService.getCurrentUser().getUser();
    Course originalCourse =
        Course.builder()
            .id(1L)
            .courseName("Original Course")
            .term("S25")
            .school("Original School")
            .instructorEmail(user.getEmail())
            .canvasApiToken("existingToken")
            .canvasCourseId("existingCourseId")
            .build();

    when(courseRepository.findById(eq(1L))).thenReturn(Optional.of(originalCourse));
    when(courseRepository.save(any(Course.class))).thenReturn(originalCourse);

    mockMvc
        .perform(put("/api/courses/updateCourseCanvasToken").param("courseId", "1").with(csrf()))
        .andExpect(status().isOk());

    assertEquals("existingToken", originalCourse.getCanvasApiToken());
    assertEquals("existingCourseId", originalCourse.getCanvasCourseId());
  }
}
