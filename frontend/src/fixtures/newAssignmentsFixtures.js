const newAssignmentsFixtures = {
  oneIndividual: [
    {
      id: 1,
      repoPrefix: "lab01",
      asnType: "INDIVIDUAL",
      visibility: "PUBLIC",
      permission: "MAINTAIN",
      createReposFor: "STUDENTS_ONLY",
      teamRegex: null,
    },
  ],
  threeAssignments: [
    {
      id: 1,
      repoPrefix: "lab01",
      asnType: "INDIVIDUAL",
      visibility: "PUBLIC",
      permission: "MAINTAIN",
      createReposFor: "STUDENTS_ONLY",
      teamRegex: null,
    },
    {
      id: 2,
      repoPrefix: "lab02",
      asnType: "INDIVIDUAL",
      visibility: "PRIVATE",
      permission: "READ",
      createReposFor: "STUDENTS_AND_STAFF",
      teamRegex: null,
    },
    {
      id: 3,
      repoPrefix: "proj-team",
      asnType: "TEAM",
      visibility: "PRIVATE",
      permission: "WRITE",
      createReposFor: null,
      teamRegex: "s26-.*",
    },
  ],
  // What POST and PUT answer with: the saved assignment and the job started
  savedWithJob: {
    assignment: {
      id: 4,
      repoPrefix: "lab04",
      asnType: "INDIVIDUAL",
      visibility: "PUBLIC",
      permission: "MAINTAIN",
      createReposFor: "STUDENTS_ONLY",
      teamRegex: null,
    },
    job: { id: 99, status: "processing" },
  },
};

export { newAssignmentsFixtures };
