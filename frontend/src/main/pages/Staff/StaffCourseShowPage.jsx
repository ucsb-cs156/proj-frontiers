import React from "react";
import InstructorCourseShowPage from "main/pages/Instructor/InstructorCourseShowPage";

export default function StaffCourseShowPage() {
  return (
    <InstructorCourseShowPage
      testId="StaffCourseShowPage"
      showSettingsTab={false}
      staffTabIsInstructor={false}
      canEditStudents={true}
      canManageTeams={true}
    />
  );
}
