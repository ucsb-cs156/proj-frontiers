import OurPagination from "main/components/Common/OurPagination";
import { useState } from "react";

export default {
  title: "components/Common/OurPagination",
  component: OurPagination,
};

const Template = (args) => {
  const [currentActivePage, setCurrentActivePage] = useState(1);
  return (
    <OurPagination
      currentActivePage={currentActivePage}
      updateActivePage={setCurrentActivePage}
      {...args}
    />
  );
};

export const Total_10 = Template.bind({});
Total_10.args = {
  totalPages: 10,
};

export const Total_5 = Template.bind({});
Total_5.args = {
  totalPages: 5,
};

export const Default = Template.bind({});
Default.args = {};
