import { useMemo } from "react";
import { toast } from "react-toastify";
import { useBackend } from "main/utils/useBackend";

/**
 * Shared onError handler for section create/update mutations. A 409 from the
 * backend means the (course, section) pair already exists.
 */
export function onSectionMutationError(error) {
  if (error.response.status === 409) {
    toast("A section with that section value already exists for this course.");
  } else {
    toast(`${error}`);
  }
}

/**
 * Fetches the course's sections (only when `enabled` is true) and returns a
 * function that translates a raw section value into its human readable
 * label. This is the single place where the "is translation enabled, and if
 * so, what is the label for this section" logic lives on the frontend, so
 * that every table/component that displays a section value (e.g.
 * RosterStudentTable, DroppedStudentsTable) behaves consistently.
 *
 * When `enabled` is false, or a section has no matching label, the raw
 * section value is returned unchanged.
 *
 * @param {number|string} courseId the id of the course
 * @param {boolean} enabled whether the TRANSLATE_SECTIONS course option is enabled
 * @returns {(section: string) => string} function that translates a raw section value
 */
export function useSectionLabels(courseId, enabled) {
  const sectionsQueryKey = `/api/courses/${courseId}/sections`;
  const { data: sections = [] } = useBackend(
    [sectionsQueryKey],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: sectionsQueryKey },
    [],
    true,
    { enabled },
  );

  const sectionLabelsBySection = useMemo(
    () => new Map(sections.map((s) => [s.section, s.label])),
    [sections],
  );

  return function translateSection(section) {
    if (!enabled || !section) {
      return section;
    }
    const label = sectionLabelsBySection.get(section);
    return label ? label : section;
  };
}
