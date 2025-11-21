import { Typeahead } from 'react-bootstrap-typeahead';
import 'react-bootstrap-typeahead/css/Typeahead.css';

export default function RosterStudentDropdown({ rosterStudents, setValue, isInvalid }) {
  const options = rosterStudents.map((student) => ({
    id: student.id,
    label: `${student.firstName} ${student.lastName}`,
  }));

  return (
    <Typeahead
      id="rosterStudentId"
      inputProps={{ "data-testid": "RosterStudentDropdown" }}
      options={options}
      placeholder="Select a student..."
      isInvalid={isInvalid}
      onChange={(selected) => {
        if (selected.length > 0) {
          setValue("rosterStudentId", selected[0].id, { shouldValidate: true });
        } else {
          setValue("rosterStudentId", "", { shouldValidate: true });
        }
      }}
    />
  );
}
