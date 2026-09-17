import { useBackend } from "main/utils/useBackend";

export function titleCaseFromOption(option) {
  return option
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

export function courseOptionsQueryKey(courseId) {
  return `/api/course/options/?courseId=${courseId}`;
}

/**
 * Fetch the map of course options (e.g. { TRANSLATE_SECTIONS: true, ... })
 * for a course. All callers share the same query key, so toggling an option
 * anywhere (e.g. on the Settings tab) refreshes every consumer.
 *
 * @param courseId id of the course
 * @param options extra react-query options (e.g. { enabled: false })
 */
export function useCourseOptions(courseId, options = {}) {
  return useBackend(
    [courseOptionsQueryKey(courseId)],
    {
      // Stryker disable next-line StringLiteral : GET and "" are equivalent
      method: "GET",
      url: "/api/course/options",
      params: { courseId },
    },
    {},
    false,
    options,
  );
}
