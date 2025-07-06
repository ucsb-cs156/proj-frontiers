const coursesFixtures = {
  threeCourses: [
    {
      id: 1,
      installationId: "123456",
      orgName: "ucsb-cs156-s25",
      courseName: "CMPSC 156",
      term: "Spring 2025",
      school: "UCSB",
    },
    {
      id: 2,
      installationId: "654321",
      orgName: "wsu-cpts489-fa20",
      courseName: "CPTS 489",
      term: "Fall 2020",
      school: "WSU",
    },
    {
      id: 3,
      installationId: "789012",
      orgName: "ucsb-cs156-f25",
      courseName: "CMPSC 156",
      term: "Fall 2025",
      school: "UCSB",
    },
  ],
  oneCourseWithEachStatus: [
    {
      id: 1,
      courseName: "CMPSC 156",
      term: "Spring 2025",
      school: "UCSB",
      status: "Pending",
    },
    {
      id: 2,
      courseName: "CPTS 489",
      term: "Fall 2020",
      school: "WSU",
      status: "Join Course",
    },
    {
      id: 3,
      courseName: "CMPSC 156",
      term: "Fall 2025",
      school: "UCSB",
      status: "Invited",
    },
    {
      id: 4,
      courseName: "CMPSC 156",
      term: "Spring 2026",
      school: "UCSB",
      status: "Member",
    },
    {
      id: 5,
      courseName: "CMPSC 148",
      term: "Spring 2026",
      school: "UCSB",
      status: "Owner",
    },
    {
      id: 6,
      courseName: "CMPSC 156",
      term: "Fall 2026",
      school: "UCSB",
      status: "Error",
    },
    {
      id: 7,
      courseName: "CMPSC 156",
      term: "Fall 2026",
      school: "UCSB",
      status: "Unknown Status",
    },
  ],
};

export default coursesFixtures;
