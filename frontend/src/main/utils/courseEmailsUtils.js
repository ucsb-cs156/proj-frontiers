/**
 * Query key prefix for every cached result of GET /api/courses/emails for a
 * course (whatever the type and format). Mutations that change who is on the
 * staff list or roster invalidate this prefix so that any Emails card showing
 * those addresses refetches them.
 */
export function courseEmailsQueryKey(courseId) {
  return `/api/courses/emails?courseId=${courseId}`;
}

// The formats GET /api/courses/emails can return the addresses in: `value` is
// the EmailFormats value the endpoint accepts, `label` is what the user sees.
// The first one is the default.
export const EMAIL_FORMATS = [
  { value: "ONE_PER_LINE", label: "One per line" },
  { value: "COMMA_SEPARATED", label: "Comma separated" },
];
