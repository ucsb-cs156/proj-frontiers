import React from "react";
import { useParams } from "react-router";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { Tab, Tabs } from "react-bootstrap";

export default function StudentCourseShowPage() {
  const { id } = useParams();

  return (
    <BasicLayout>
      <div className="pt-2">
        <div className="border rounded-3 p-4 mb-4">
          <h1 className="h3 mb-0">Student Course: {id}</h1>
        </div>
        <Tabs defaultActiveKey="placeholder">
          <Tab eventKey="placeholder" title="Placeholder" className="pt-2">
            <div className="mt-3">
              <p>More features coming soon.</p>
            </div>
          </Tab>
        </Tabs>
      </div>
    </BasicLayout>
  );
}
