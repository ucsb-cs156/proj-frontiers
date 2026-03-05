import { useBackend } from "main/utils/useBackend";
import { Typeahead } from "react-bootstrap-typeahead";
import { Controller } from "react-hook-form";

export function SchoolTypeahead({ control, rules, testid }) {
  const { data: schools = [] } = useBackend(
    [`/api/systemInfo/schools`],
    {
      method: "GET",
      url: `/api/systemInfo/schools`,
    },
    undefined,
    true,
    {
      staleTime: "static",
    },
  );

  const filterByFields = (option, props) => {
    const search = props.text.toLowerCase();
    return (
      option.displayName.toLowerCase().includes(search) ||
      option.alternateNames.some((name) => name.toLowerCase().includes(search))
    );
  };
  return (
    <Controller
      control={control}
      name="school"
      rules={rules}
      render={({ field, fieldState }) => (
        <Typeahead
          selected={field.value ? [field.value] : []}
          onChange={(selected) => field.onChange(selected[0] ?? null)}
          id="school-typeahead"
          isInvalid={fieldState.invalid}
          inputProps={{
            "aria-label": "Choose a school",
            "data-testid": testid,
          }}
          options={schools}
          labelKey="displayName"
          filterBy={filterByFields}
          placeholder="Start typing to select a school..."
        />
      )}
    />
  );
}
