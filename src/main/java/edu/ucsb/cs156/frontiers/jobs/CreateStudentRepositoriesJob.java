package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;

@Builder
public class CreateStudentRepositoriesJob implements JobContextConsumer {
    Course course;
    RepositoryService repositoryService;
    String repositoryPrefix;
    Boolean isPrivate;

    @Override
    public void accept(JobContext ctx) throws Exception {
        ctx.log("Processing...");
        for(RosterStudent student : course.getRosterStudents()){
            if(student.getGithubLogin() != null && student.getOrgStatus() == OrgStatus.MEMBER){
                repositoryService.createStudentRepository(course, student, repositoryPrefix, isPrivate);
            }
        }
        ctx.log("Done");
    }
}
