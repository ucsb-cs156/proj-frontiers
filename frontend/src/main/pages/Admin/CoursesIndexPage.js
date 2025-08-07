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
    ["/api/courses/allForAdmins"],
    { method: "GET", url: "/api/courses/allForAdmins" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  const [viewModal, setViewModal] = React.useState(false);
  const [courseToEdit, setCourseToEdit] = React.useState(null);

  const createObjectToAxiosParams = (course) => ({
    url: "/api/courses/post",
    method: "POST",
    params: {
      courseName: course.courseName,
      term: course.term,
      school: course.school,
    },
  });

  const updateObjectToAxiosParams = (course) => ({
    url: "/api/courses",
    method: "PUT",
    params: {
      courseId: course.id,
      courseName: course.courseName,
      term: course.term,
      school: course.school,
    },
  });

  const onCreateSuccess = (course) => {
    toast(`Course ${course.courseName} created`);
    setViewModal(false);
  };

  const onUpdateSuccess = (course) => {
    toast(`Course ${course.courseName} updated`);
    setViewModal(false);
    setCourseToEdit(null);
  };

  const createMutation = useBackendMutation(
    createObjectToAxiosParams,
    { onSuccess: onCreateSuccess },
    // Stryker disable next-line all : hard to set up test for caching
    ["/api/courses/all"],
  );

  const updateMutation = useBackendMutation(
    updateObjectToAxiosParams,
    { onSuccess: onUpdateSuccess },
    // Stryker disable next-line all : hard to set up test for caching
    ["/api/courses/allForAdmins"],
  );

  const onSubmit = async (data) => {
    if (courseToEdit) {
      updateMutation.mutate({
        ...data,
        id: courseToEdit.id
      });
    } else {
      createMutation.mutate(data);
    }
  };

  const createCourse = () => {
    setCourseToEdit(null);
    setViewModal(true);
  };

  const editCourse = (course) => {
    setCourseToEdit(course);
    setViewModal(true);
  };

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
          initialContents={courseToEdit}
          buttonText={courseToEdit ? "Update" : "Create"}
        />
        <InstructorCoursesTable 
          courses={courses} 
          currentUser={currentUser} 
          onEditCourse={editCourse}
        />
      </div>
    </BasicLayout>
  );
}
