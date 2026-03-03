const jobsFixtures = {
  threeJobs: [
    {
      id: 1,
      jobName: "MembershipAuditJob",
      createdBy: { email: "user1@example.com" },
      course: { courseName: "CS 101" },
      createdAt: "2023-01-01T10:00:00",
      updatedAt: "2023-01-01T10:05:00",
      status: "complete",
      log: "Job completed successfully",
    },
    {
      id: 2,
      jobName: "DataSyncJob",
      createdBy: { email: "user2@example.com" },
      course: { courseName: "CS 102" },
      createdAt: "2023-01-02T10:00:00",
      updatedAt: "2023-01-02T10:05:00",
      status: "error",
      log: "Job failed with error: Connection timeout",
    },
    {
      id: 3,
      jobName: "UpdateAllJob",
      createdBy: { email: "user3@example.com" },
      course: { courseName: "CS 103" },
      createdAt: "2023-01-03T10:00:00",
      updatedAt: "2023-01-03T10:05:00",
      status: "running",
      log: "Job is currently running...",
    },
  ],
  oneJob: [
    {
      id: 1,
      jobName: "MembershipAuditJob",
      createdBy: { email: "user1@example.com" },
      course: { courseName: "CS 101" },
      createdAt: "2023-01-01T10:00:00",
      updatedAt: "2023-01-01T10:05:00",
      status: "complete",
      log: "Job completed successfully",
    },
  ],
  longLogJob: [
    {
      id: 1,
      jobName: "LongLogJob",
      createdBy: { email: "user1@example.com" },
      course: { courseName: "CS 101" },
      createdAt: "2023-01-01T10:00:00",
      updatedAt: "2023-01-01T10:05:00",
      status: "complete",
      log: "This is a very long log message that should be displayed in a scrollable container. ".repeat(
        20,
      ),
    },
  ],
};

export { jobsFixtures };
