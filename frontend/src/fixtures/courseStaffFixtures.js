const courseStaffFixtures = {
  oneStaff: [
    {
      id: 1,
      firstName: "Dr. John",
      lastName: "Professor",
      email: "johnprof@ucsb.edu",
    },
  ],

  threeStaff: [
    {
      id: 1,
      firstName: "Dr. John",
      lastName: "Professor",
      email: "johnprof@ucsb.edu",
    },

    {
      id: 2,
      firstName: "Dr. Jane",
      lastName: "Instructor",
      email: "janeinstr@ucsb.edu",
    },

    {
      id: 3,
      firstName: "Mark",
      lastName: "TA",
      email: "markta@ucsb.edu",
    },
  ],
  staffWithEachStatus: [
    {
      id: 1,
      firstName: "Dr. John",
      lastName: "Professor",
      email: "johnprof@ucsb.edu",
      githubLogin: null,
      orgStatus: "PENDING",
    },

    {
      id: 2,
      firstName: "Dr. Jane",
      lastName: "Instructor",
      email: "janeinstr@ucsb.edu",
      githubLogin: null,
      orgStatus: "JOINCOURSE",
    },

    {
      id: 3,
      firstName: "Mark",
      lastName: "TA",
      email: "markta@ucsb.edu",
      githubLogin: null,
      orgStatus: "INVITED",
    },
    {
      id: 4,
      firstName: "Sarah",
      lastName: "Lead",
      email: "sarahlead@ucsb.edu",
      githubLogin: "sarahlead",
      orgStatus: "OWNER",
    },
    {
      id: 5,
      firstName: "Mike",
      lastName: "Assistant",
      email: "mikeassist@ucsb.edu",
      githubLogin: "mikeassist",
      orgStatus: "MEMBER",
    },
  ],
};

export { courseStaffFixtures };
