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
      orgStatus: "PENDING",
    },

    {
      id: 2,
      studentId: "X123456",
      firstName: "Tom",
      lastName: "Hanks",
      email: "tomhanks@ucsb.edu",
      orgStatus: "JOINCOURSE",
    },

    {
      id: 3,
      studentId: "Z123456",
      firstName: "Emma",
      lastName: "Watson",
      email: "emmawatson@ucsb.edu",
      orgStatus: "INVITED",
    },
    {
      id: 4,
      studentId: "B123456",
      firstName: "Jon",
      lastName: "Snow",
      email: "jonsnow@ucsb.edu",
      orgStatus: "OWNER",
    },
    {
      id: 5,
      studentId: "C123456",
      firstName: "Bob",
      lastName: "Smith",
      email: "bobsmith@ucsb.edu",
      orgStatus: "MEMBER",
    },
    {
      id: 6,
      studentId: "D123456",
      firstName: "Arya",
      lastName: "Sue",
      email: "aryasue@ucsb.edu",
      orgStatus: "Illegal status that will never occur",
    },
  ],
};

export { rosterStudentFixtures };
