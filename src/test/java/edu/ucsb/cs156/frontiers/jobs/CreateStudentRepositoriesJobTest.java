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
    public void testCreateStudentRepository_public() throws Exception {
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        RosterStudent student = RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
        course.setRosterStudents(List.of(student));

        doNothing().when(service).createStudentRepository(eq(course), eq(student), contains("repo-prefix"), eq(false));

        var repoJob = spy(CreateStudentRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(false)
                .build());

        repoJob.accept(ctx);
        String expected = """
                Processing...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(service, times(1)).createStudentRepository(eq(course), eq(student), contains("repo-prefix"), eq(false));
    }

    @Test
    public void testCreateStudentRepository_private() throws Exception {
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        RosterStudent student = RosterStudent.builder().githubLogin("studentLogin").orgStatus(OrgStatus.MEMBER).build();
        course.setRosterStudents(List.of(student));

        doNothing().when(service).createStudentRepository(eq(course), eq(student), contains("repo-prefix"), eq(true));

        var repoJob = spy(CreateStudentRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .course(course)
                .isPrivate(true)
                .build());

        repoJob.accept(ctx);
        String expected = """
                Processing...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(service, times(1)).createStudentRepository(eq(course), eq(student), contains("repo-prefix"), eq(true));
    }


    @Test
    public void expectDoesntCallForNoLogin() throws Exception {
        Course course = Course.builder().orgName("ucsb-cs156").installationId("1234").build();
        RosterStudent student = RosterStudent.builder().build();
        course.setRosterStudents(List.of(student));

        var repoJob = spy(CreateStudentRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .isPrivate(false)
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
        RosterStudent student = RosterStudent.builder().githubLogin("banana").orgStatus(OrgStatus.NONE).build();
        course.setRosterStudents(List.of(student));
        var repoJob = spy(CreateStudentRepositoriesJob.builder()
                .repositoryService(service)
                .repositoryPrefix("repo-prefix")
                .isPrivate(false)
                .course(course)
                .build());

        repoJob.accept(ctx);
        String expected = """
                Processing...
                Done""";
        assertEquals(expected, jobStarted.getLog());

        verify(service, times(0)).createStudentRepository(any(),any(),any(),any());
    }
}
