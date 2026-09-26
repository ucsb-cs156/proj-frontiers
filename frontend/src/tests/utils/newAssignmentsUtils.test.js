import { beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";
import {
  ASSIGNMENT_TYPE_LABELS,
  CREATE_REPOS_FOR_LABELS,
  DEFAULT_TEAM_REGEX,
  JOBS_QUERY_KEY,
  NEW_ASSIGNMENTS_URL,
  PERMISSION_LABELS,
  SIGNED_COMMITS_REQUIRED_ICON,
  VISIBILITY_LABELS,
  assignmentToFormData,
  assignmentsQueryKey,
  formDataToParams,
  jobLaunchedMessage,
  jobStartedMessage,
  onAssignmentMutationError,
} from "main/utils/newAssignmentsUtils";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

describe("newAssignmentsUtils tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("urls and query keys", () => {
    expect(NEW_ASSIGNMENTS_URL).toBe("/api/assignments");
    expect(assignmentsQueryKey(7)).toBe("/api/assignments?courseId=7");
    expect(assignmentsQueryKey("12")).toBe("/api/assignments?courseId=12");
    expect(JOBS_QUERY_KEY).toBe("/api/jobs/course");
    expect(DEFAULT_TEAM_REGEX).toBe(".*");
  });

  test("labels", () => {
    expect(ASSIGNMENT_TYPE_LABELS).toEqual({
      INDIVIDUAL: "Individual",
      TEAM: "Team",
    });
    expect(VISIBILITY_LABELS).toEqual({ PUBLIC: "Public", PRIVATE: "Private" });
    expect(PERMISSION_LABELS).toEqual({
      READ: "Read",
      WRITE: "Write",
      MAINTAIN: "Maintain",
      ADMIN: "Admin",
    });
    expect(CREATE_REPOS_FOR_LABELS).toEqual({
      STUDENTS_ONLY: "Students Only",
      STAFF_ONLY: "Staff Only",
      STUDENTS_AND_STAFF: "Students and Staff",
    });
  });

  test("formDataToParams for an individual assignment sends createReposFor, not teamRegex", () => {
    expect(
      formDataToParams("INDIVIDUAL", {
        repoPrefix: "lab01",
        isPrivate: true,
        permission: "READ",
        requireSignedCommit: true,
        createReposFor: "STAFF_ONLY",
        teamRegex: "ignored",
      }),
    ).toEqual({
      repoPrefix: "lab01",
      visibility: "PRIVATE",
      permission: "READ",
      requireSignedCommit: true,
      createReposFor: "STAFF_ONLY",
    });
  });

  test("formDataToParams for a team assignment sends teamRegex, not createReposFor", () => {
    expect(
      formDataToParams("TEAM", {
        repoPrefix: "proj",
        isPrivate: false,
        permission: "ADMIN",
        requireSignedCommit: false,
        createReposFor: "ignored",
        teamRegex: "s26-.*",
      }),
    ).toEqual({
      repoPrefix: "proj",
      visibility: "PUBLIC",
      permission: "ADMIN",
      requireSignedCommit: false,
      teamRegex: "s26-.*",
    });
  });

  test("formDataToParams sends requireSignedCommit as false when the form has no value for it", () => {
    ["INDIVIDUAL", "TEAM"].forEach((asnType) => {
      expect(
        formDataToParams(asnType, { repoPrefix: "x", permission: "READ" })
          .requireSignedCommit,
      ).toBe(false);
    });
  });

  test("the signed commits checkmark is the white heavy check mark emoji", () => {
    expect(SIGNED_COMMITS_REQUIRED_ICON).toBe("\u2705");
    expect(SIGNED_COMMITS_REQUIRED_ICON).toBe("✅");
  });

  test("assignmentToFormData", () => {
    expect(
      assignmentToFormData({
        id: 3,
        repoPrefix: "proj-team",
        asnType: "TEAM",
        visibility: "PRIVATE",
        permission: "WRITE",
        requireSignedCommit: true,
        createReposFor: null,
        teamRegex: "s26-.*",
      }),
    ).toEqual({
      repoPrefix: "proj-team",
      isPrivate: true,
      permission: "WRITE",
      requireSignedCommit: true,
      createReposFor: null,
      teamRegex: "s26-.*",
    });
    expect(
      assignmentToFormData({
        repoPrefix: "lab01",
        visibility: "PUBLIC",
        permission: "READ",
        createReposFor: "STAFF_ONLY",
        teamRegex: null,
      }).isPrivate,
    ).toBe(false);
  });

  test("jobStartedMessage", () => {
    expect(jobStartedMessage("created", { job: { id: 99 } })).toBe(
      "Assignment created. Job 99 started to create its repositories; click the job number in the table to watch its log.",
    );
    expect(jobStartedMessage("updated", { job: { id: 5 } })).toBe(
      "Assignment updated. Job 5 started to create its repositories; click the job number in the table to watch its log.",
    );
  });

  test("jobLaunchedMessage", () => {
    expect(jobLaunchedMessage({ job: { id: 99 } })).toBe(
      "Job 99 started to create the repositories of this assignment; click the job number in the table to watch its log.",
    );
  });

  test("onAssignmentMutationError shows the message from the backend", () => {
    onAssignmentMutationError({
      response: { data: { message: "repoPrefix must not be blank" } },
    });
    expect(toast).toHaveBeenCalledWith("repoPrefix must not be blank");
    expect(toast).toHaveBeenCalledTimes(1);
  });

  test("onAssignmentMutationError shows the error itself when there is no message", () => {
    onAssignmentMutationError(new Error("Network Error"));
    expect(toast).toHaveBeenCalledWith("Error: Network Error");

    const noData = new Error("boom");
    noData.response = {};
    onAssignmentMutationError(noData);
    expect(toast).toHaveBeenCalledWith("Error: boom");
    expect(toast).toHaveBeenCalledTimes(2);
  });
});
