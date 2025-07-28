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

export const Total_10_Max_5 = Template.bind({});
Total_10_Max_5.args = {
  totalPages: 10,
  maxPages: 5,
};

export const Total_5_Max_10 = Template.bind({});
Total_5_Max_10.args = {
  totalPages: 5,
  maxPages: 10,
};
