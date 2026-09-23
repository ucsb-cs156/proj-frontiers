import { Form } from "react-bootstrap";
import { useBackend } from "main/utils/useBackend";

/**
 * Shows the translation (label) of a raw section value, along with a
 * dropdown that lets the user pick one of the course's known sections.
 * Selecting a section from the dropdown updates the raw section value
 * (via onChange) and, in turn, the translation shown.
 */
function SectionTranslator({
  courseId,
  section,
  onChange,
  testIdPrefix = "SectionTranslator",
}) {
  const sectionsQueryKey = `/api/courses/${courseId}/sections`;

  const { data: sections = [] } = useBackend(
    [sectionsQueryKey],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: sectionsQueryKey },
    [],
    true,
    { enabled: Boolean(courseId) },
  );

  const matchingSection = sections.find((s) => s.section === section);
  const translation = matchingSection ? matchingSection.label : "";

  const handleSelectChange = (event) => {
    onChange(event.target.value);
  };

  return (
    <Form.Group className="mb-3">
      <Form.Label htmlFor={`${testIdPrefix}-translation`}>
        Section Translation
      </Form.Label>
      <Form.Control
        data-testid={`${testIdPrefix}-translation`}
        id={`${testIdPrefix}-translation`}
        type="text"
        value={translation}
        disabled
        readOnly
      />
      <Form.Label htmlFor={`${testIdPrefix}-select`} className="mt-2">
        Select Section
      </Form.Label>
      <Form.Select
        data-testid={`${testIdPrefix}-select`}
        id={`${testIdPrefix}-select`}
        value={section ?? ""}
        onChange={handleSelectChange}
      >
        <option value="">-- Select a Section --</option>
        {sections.map((s) => (
          <option key={s.id} value={s.section}>
            {s.section} - {s.label}
          </option>
        ))}
      </Form.Select>
    </Form.Group>
  );
}

export default SectionTranslator;
