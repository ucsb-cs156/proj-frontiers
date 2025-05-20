import coursesFixtures from "./coursesFixtures";
import usersFixtures from "./usersFixtures";

const RosterStudentsFixtures = {
    threeRosterStudents: [
      {
        id: 1,
        course: coursesFixtures.threeCourses[0],
        studentId: "9627X84",
        firstName: "Shuang",
        lastName: "Li",
        email: "shuang@ucsb.edu",
        user: usersFixtures.threeUsers[0],
        rosterStatus: "ENROLLED",
        orgStatus: "INVITED",
        githubId: 123456,
        githubLogin: "aliceGH",
      },
      {
        id: 2,
        course: coursesFixtures.threeCourses[0],
        studentId: "5888Y89",
        firstName: "Andrew",
        lastName: "Green",
        email: "andrew@ucsb.edu",
        user: usersFixtures.threeUsers[1],
        rosterStatus: "DROPPED",
        orgStatus: "NONE",
        githubId: 537424,
        githubLogin: "bobGH",
      },
      {
        id: 3,
        course: coursesFixtures.threeCourses[1],
        studentId: "6666P11",
        firstName: "Wendy",
        lastName: "Song",
        email: "wendy@ucsb.edu",
        user: usersFixtures.threeUsers[2],
        rosterStatus: "ROSTER",
        orgStatus: "MEMBER",
        githubId: null,
        githubLogin: null,
      },
    ],
  };
  
  export default RosterStudentsFixtures;
  