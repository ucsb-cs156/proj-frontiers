import React from "react";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { Tab, Tabs } from "react-bootstrap";
import RepositorySelectionForm from "main/components/RepositorySelectionForm";
import { useBackend } from "main/utils/useBackend";

export default function ArtifactSelectionPage() {
  // /api/collections/list does NOT exist yet. mvp for wireframes.
  const {
    data: collections,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/collections/list"],
    // Stryker disable next-line StringLiteral : The default value for an empty ("") method is GET. Therefore, there is no way to kill a mutation that transforms "GET" to ""
    { method: "GET", url: "/api/collections/list" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );
  return (
    <BasicLayout>
      <Tabs defaultActiveKey={"select_repos"}>
        <Tab
          eventKey={"select_repos"}
          title={"Select Repositories"}
          className="pt-2"
        >
          <RepositorySelectionForm collections={collections} />
        </Tab>
        <Tab
          eventKey={"select_artifacts"}
          title={"Select Artifacts"}
          className="pt-2"
        >
          <h1>Coming Soon</h1>
        </Tab>
      </Tabs>
    </BasicLayout>
  );
}
