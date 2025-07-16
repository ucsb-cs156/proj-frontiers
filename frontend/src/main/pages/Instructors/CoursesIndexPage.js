import React from "react";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import { useCurrentUser } from "main/utils/currentUser";
import { Button } from "react-bootstrap";
import { toast } from "react-toastify";
import CourseModal from "main/components/Courses/CourseModal";

export default function CoursesIndexPage() {
  const currentUser = useCurrentUser();

  const {
    data: courses,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/all"],
    { method: "GET", url: "/api/courses/all" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  const [viewModal, setViewModal] = React.useState(false);

  const objectToAxiosParams = (course) => ({
    url: "/api/courses/post",
    method: "POST",
    params: {
      courseName: course.courseName,
      term: course.term,
      school: course.school,
    },
  });

  const onSuccess = (course) => {
    toast(`Course ${course.courseName} created`);
    setViewModal(false);
  };

  const mutation = useBackendMutation(
    objectToAxiosParams,
    { onSuccess },
    // Stryker disable next-line all : hard to set up test for caching
    ["/api/courses/all"],
  );

  const onSubmit = async (data) => {
    mutation.mutate(data);
  };

  const createCourse = () => setViewModal(true);

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Courses</h1>
        <Button
          onClick={createCourse}
          style={{ float: "right", marginBottom: 10 }}
          variant="primary"
        >
          Create Course
        </Button>
        <CourseModal
          showModal={viewModal}
          toggleShowModal={setViewModal}
          onSubmitAction={onSubmit}
        />
        <InstructorCoursesTable courses={courses} currentUser={currentUser} />
      </div>
    </BasicLayout>
  );
}
