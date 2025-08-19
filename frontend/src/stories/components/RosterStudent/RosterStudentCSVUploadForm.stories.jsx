import RosterStudentCSVUploadForm from "main/components/RosterStudent/RosterStudentCSVUploadForm";

export default {
  title: "components/RosterStudent/RosterStudentCSVUploadForm",
  component: RosterStudentCSVUploadForm,
};

const Template = (args) => {
  return <RosterStudentCSVUploadForm {...args} />;
};

export const Default = Template.bind({});

Default.args = {
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
