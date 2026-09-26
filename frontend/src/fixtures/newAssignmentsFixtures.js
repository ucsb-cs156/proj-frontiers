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
      lastJobId: 12,
      requireSignedCommit: false,
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
      lastJobId: 12,
      requireSignedCommit: false,
    },
    {
      id: 2,
      repoPrefix: "lab02",
      asnType: "INDIVIDUAL",
      visibility: "PRIVATE",
      permission: "READ",
      createReposFor: "STUDENTS_AND_STAFF",
      teamRegex: null,
      lastJobId: null,
      requireSignedCommit: true,
    },
    {
      id: 3,
      repoPrefix: "proj-team",
      asnType: "TEAM",
      visibility: "PRIVATE",
      permission: "WRITE",
      createReposFor: null,
      teamRegex: "s26-.*",
      lastJobId: 15,
      requireSignedCommit: true,
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
      lastJobId: 99,
      requireSignedCommit: false,
    },
    job: { id: 99, status: "processing" },
  },
};

export { newAssignmentsFixtures };
