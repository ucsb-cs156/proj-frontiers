package edu.ucsb.cs156.frontiers.jobs;


import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateStudentRepositoriesJobTest {

    @Mock
    private RepositoryService service;

    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateStudentRepository() throws Exception {
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        User user = User.builder().githubLogin("studentLogin").build();
        RosterStudent student = RosterStudent.builder().user(user).orgStatus(OrgStatus.MEMBER).build();
        course.setRosterStudents(List.of(student));

        doNothing().when(service).createStudentRepository(contains("1234"), contains("ucsb-cs156"), eq(student), contains("repo-prefix"));

        var repoJob = spy(CreateStudentRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .build());

        repoJob.accept(ctx);
        String expected = """
                Processing...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(service, times(1)).createStudentRepository(contains("1234"), contains("ucsb-cs156"), eq(student), contains("repo-prefix"));
    }

    @Test
    public void expectDoesntCallForNoUser() throws Exception {
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        RosterStudent student = RosterStudent.builder().build();
        course.setRosterStudents(List.of(student));
        var repoJob = spy(CreateStudentRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .build());

        repoJob.accept(ctx);
        String expected = """
                Processing...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(service, times(0)).createStudentRepository(any(),any(),any(),any());
    }

    @Test
    public void expectDoesntCallForNoLogin() throws Exception {
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        User user = User.builder().build();
        RosterStudent student = RosterStudent.builder().user(user).build();
        course.setRosterStudents(List.of(student));

        var repoJob = spy(CreateStudentRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .build());

        repoJob.accept(ctx);
        String expected = """
                Processing...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(service, times(0)).createStudentRepository(any(),any(),any(),any());
    }

    @Test
    public void expectDoesntCallForNotMember() throws Exception {
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        RosterStudent student = RosterStudent.builder().orgStatus(OrgStatus.NONE).build();
        course.setRosterStudents(List.of(student));
        var repoJob = spy(CreateStudentRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .build());

        repoJob.accept(ctx);
        String expected = """
                Processing...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(service, times(0)).createStudentRepository(any(), any(), any(), any());
    }
}
