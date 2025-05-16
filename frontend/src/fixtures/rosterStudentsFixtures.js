import coursesFixtures from "./coursesFixtures";
import usersFixtures from "./usersFixtures";

const rosterStudentsFixtures = {
  // three roster students from the same course.
  threeRosterStudents: [
    {
      id: 1,
      course: coursesFixtures.threeCourses[0],
      studentID: "1234X67",
      firstName: "Phill",
      lastName: "Conrad",
      email: "phtcon@ucsb.edu",
      user: usersFixtures.threeUsers[0],
      rosterStatus: "MANUAL",
      orgStatus: "NONE",
      githubId: null,
      githubLogin: null,
    },
    {
      id: 2,
      course: coursesFixtures.threeCourses[0],
      studentID: "XM43KJ3",
      firstName: "Phillip",
      lastName: "Conrad",
      email: "pconrad.cis@gmail.com",
      user: usersFixtures.threeUsers[1],
      rosterStatus: "ROSTER",
      orgStatus: "MEMBER",
      githubId: 123456789,
      githubLogin: "pconrad",
    },
    {
      id: 3,
      course: coursesFixtures.threeCourses[0],
      studentID: "ZZZZZZZ",
      firstName: "Craig",
      lastName: "Zzyxx",
      email: "craig.zzyzx@example.org",
      user: usersFixtures.threeUsers[2],
      rosterStatus: "DROPPED",
      orgStatus: "NONE",
      githubId: null,
      githubLogin: null,
    },
  ],
};

export default rosterStudentsFixtures;
