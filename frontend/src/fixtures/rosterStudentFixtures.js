const rosterStudentFixtures = {
  oneStudent: [
    {
      id: 1,
      studentId: "1234567",
      firstName: "Bob",
      lastName: "Smith",
      email: "bobsmith@ucsb.edu",
    },
  ],

  threeStudents: [
    {
      id: 3,
      studentId: "A123456",
      firstName: "Alice",
      lastName: "Brown",
      email: "alicebrown@ucsb.edu",
    },

    {
      id: 4,
      studentId: "X123456",
      firstName: "Tom",
      lastName: "Hanks",
      email: "tomhanks@ucsb.edu",
    },

    {
      id: 6,
      studentId: "Z123456",
      firstName: "Emma",
      lastName: "Watson",
      email: "emmawatson@ucsb.edu",
    },
  ],
  studentsWithEachStatus: [
    {
      id: 1,
      studentId: "A123456",
      firstName: "Alice",
      lastName: "Brown",
      email: "alicebrown@ucsb.edu",
      githubLogin: null,
      orgStatus: "PENDING",
    },

    {
      id: 2,
      studentId: "X123456",
      firstName: "Tom",
      lastName: "Hanks",
      email: "tomhanks@ucsb.edu",
      githubLogin: null,
      orgStatus: "JOINCOURSE",
    },

    {
      id: 3,
      studentId: "Z123456",
      firstName: "Emma",
      lastName: "Watson",
      email: "emmawatson@ucsb.edu",
      githubLogin: null,
      orgStatus: "INVITED",
    },
    {
      id: 4,
      studentId: "B123456",
      firstName: "Jon",
      lastName: "Snow",
      email: "jonsnow@ucsb.edu",
      githubLogin: "jonsnow",
      orgStatus: "OWNER",
    },
    {
      id: 5,
      studentId: "C123456",
      firstName: "Bob",
      lastName: "Smith",
      email: "bobsmith@ucsb.edu",
      githubLogin: "bobsmith",
      orgStatus: "MEMBER",
    },
    {
      id: 6,
      studentId: "D123456",
      firstName: "Arya",
      lastName: "Sue",
      email: "aryasue@ucsb.edu",
      githubLogin: "aryasue",
      orgStatus: "Illegal status that will never occur",
    },
  ],
};

const loadResultFixtures = {
  successful: {
    created: 1,
    updated: 2,
    rejected: [],
  },
  failed: {
    created: 3,
    updated: 4,
    rejected: rosterStudentFixtures.oneStudent,
  },
};

export { rosterStudentFixtures, loadResultFixtures };
