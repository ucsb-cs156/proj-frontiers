const jobsFixtures = {
  threeJobs: [
    {
      id: 1,
      jobName: "MembershipAuditJob",
      createdAt: "2023-01-01T10:00:00",
      updatedAt: "2023-01-01T10:05:00",
      status: "complete",
      log: "Job completed successfully",
    },
    {
      id: 2,
      jobName: "DataSyncJob",
      createdAt: "2023-01-02T10:00:00",
      updatedAt: "2023-01-02T10:05:00",
      status: "error",
      log: "Job failed with error: Connection timeout",
    },
    {
      id: 3,
      jobName: "UpdateAllJob",
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
