import { useBackend } from "main/utils/useBackend";
import { Typeahead } from "react-bootstrap-typeahead";
import { Controller } from "react-hook-form";

export function SchoolTypeahead({ control, rules }) {
  const { data: schools } = useBackend(
    [`/api/systemInfo/schools`],
    {
      method: "GET",
      url: `/api/systemInfo/schools`,
    },
    undefined,
    true,
    {
      placeholderData: [],
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
          onInputChange={() => {
            if (field.value) field.onChange(null);
          }}
          id="school"
          isInvalid={fieldState.invalid}
          options={schools}
          labelKey="displayName"
          filterBy={filterByFields}
          placeholder="Start typing to select a school..."
        />
      )}
    />
  );
}
