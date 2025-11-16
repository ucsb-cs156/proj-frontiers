import { Typeahead } from "react-bootstrap-typeahead";
import { Form } from "react-bootstrap";
import "react-bootstrap-typeahead/css/Typeahead.css";

export default function RosterStudentDropdown({
  rosterStudents,
  value,
  onChange,
  isInvalid,
}) {
  const options = rosterStudents.map((student) => ({
    id: student.id,
    fullName: `${student.firstName} ${student.lastName}`,
  }));

  const selectedOption = options.filter((opt) => opt.id === value);

  const handleSelectionChange = (selected) => {
    const id = selected.length > 0 ? selected[0].id : "";
    onChange(id);
  };

  return (
    <Form.Group controlId="rosterStudentId">
      <Typeahead
        id="rosterStudentId-typeahead"
        options={options}
        labelKey="fullName"
        placeholder="Select a student."
        selected={selectedOption}
        onChange={handleSelectionChange}
        highlightOnlyResult
        onInputChange={(text) => onChange(text)}
        inputProps={{
          "aria-label": "Select Student",
          "data-testid": "RosterStudentDropdown",
          className: isInvalid ? "form-control is-invalid" : "form-control",
        }}
      />
    </Form.Group>
  );
}
