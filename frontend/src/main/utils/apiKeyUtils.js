export const API_KEYS_DOCS_URL =
  "https://github.com/ucsb-cs156/proj-frontiers/blob/main/docs/api-keys.md";

/**
 * Query key for the list of a course's API keys. Shared by the table and the
 * create/revoke mutations, so that either refreshes the list.
 */
export function apiKeysQueryKey(courseId) {
  return `/api/courses/key?courseId=${courseId}`;
}

/**
 * A ready-to-paste example request for the new-key modal: the student lookup
 * endpoint that autograders use, against this deployment, with the course id
 * and the key filled in.
 */
export function studentInfoCurlExample(courseId, key) {
  return (
    `curl -s '${window.location.origin}/api/courses/studentInfo?courseId=${courseId}&email=cgaucho@ucsb.edu' \\\n` +
    `  -H 'X-API-KEY: ${key}'`
  );
}

const MS_PER_DAY = 24 * 60 * 60 * 1000;

/**
 * Whole days from now until the timestamp, rounded up (so anything later today
 * counts as 1). Negative once the timestamp has passed.
 */
export function daysUntil(timestamp, now = new Date()) {
  return Math.ceil((new Date(timestamp) - now) / MS_PER_DAY);
}

/** An active key that expires within this many days gets a warning badge. */
export const EXPIRY_WARNING_DAYS = 14;

/** How a key is referred to in messages: its suffix, plus its label if it has one. */
export function describeKey(apiKey) {
  return apiKey.label
    ? `…${apiKey.keySuffix} (${apiKey.label})`
    : `…${apiKey.keySuffix}`;
}
