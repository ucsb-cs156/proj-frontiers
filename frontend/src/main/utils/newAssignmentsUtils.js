import { toast } from "react-toastify";

export const NEW_ASSIGNMENTS_URL = "/api/assignments";

/** Query key for the list of a course's assignments. */
export function assignmentsQueryKey(courseId) {
  return `${NEW_ASSIGNMENTS_URL}?courseId=${courseId}`;
}

// Creating or editing an assignment starts a job, which shows up on the Jobs tab
export const JOBS_QUERY_KEY = "/api/jobs/course";

export const ASSIGNMENT_TYPE_LABELS = {
  INDIVIDUAL: "Individual",
  TEAM: "Team",
};

export const VISIBILITY_LABELS = {
  PUBLIC: "Public",
  PRIVATE: "Private",
};

export const PERMISSION_LABELS = {
  READ: "Read",
  WRITE: "Write",
  MAINTAIN: "Maintain",
  ADMIN: "Admin",
};

export const CREATE_REPOS_FOR_LABELS = {
  STUDENTS_ONLY: "Students Only",
  STAFF_ONLY: "Staff Only",
  STUDENTS_AND_STAFF: "Students and Staff",
};

/** Shown in the table for an assignment whose repositories require signed commits. */
export const SIGNED_COMMITS_REQUIRED_ICON = "\u2705";

/** The team regular expression that matches every team. */
export const DEFAULT_TEAM_REGEX = ".*";

/**
 * Converts what the create/edit forms hold into the parameters that the
 * assignments endpoints take. Only the fields that apply to the type of the
 * assignment are sent: createReposFor for INDIVIDUAL, teamRegex for TEAM.
 *
 * @param {"INDIVIDUAL"|"TEAM"} asnType the type of the assignment
 * @param {object} formData what the form holds
 */
export function formDataToParams(asnType, formData) {
  const params = {
    repoPrefix: formData.repoPrefix,
    visibility: formData.isPrivate ? "PRIVATE" : "PUBLIC",
    permission: formData.permission,
    requireSignedCommit: Boolean(formData.requireSignedCommit),
  };
  if (asnType === "TEAM") {
    params.teamRegex = formData.teamRegex;
  } else {
    params.createReposFor = formData.createReposFor;
  }
  return params;
}

/** Converts an assignment into what the create/edit forms hold. */
export function assignmentToFormData(assignment) {
  return {
    repoPrefix: assignment.repoPrefix,
    isPrivate: assignment.visibility === "PRIVATE",
    permission: assignment.permission,
    requireSignedCommit: assignment.requireSignedCommit,
    createReposFor: assignment.createReposFor,
    teamRegex: assignment.teamRegex,
  };
}

/** The message toasted when an assignment has been saved and its job started. */
export function jobStartedMessage(verb, data) {
  return `Assignment ${verb}. Job ${data.job.id} started to create its repositories; click the job number in the table to watch its log.`;
}

/** The message toasted when the job of an existing assignment has been started. */
export function jobLaunchedMessage(data) {
  return `Job ${data.job.id} started to create the repositories of this assignment; click the job number in the table to watch its log.`;
}

/** Shared onError handler for assignment mutations: shows the backend's message when it has one. */
export function onAssignmentMutationError(error) {
  toast(error.response?.data?.message ?? `${error}`);
}
