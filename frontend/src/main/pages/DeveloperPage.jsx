import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import JsonView from "@uiw/react-json-view";
import { useSystemInfo } from "main/utils/systemInfo";

import Table from "react-bootstrap/Table";

const DeveloperPage = () => {
  const { data: systemInfo } = useSystemInfo();
  return (
    <BasicLayout>
      <h1>Developer Information</h1>
      <h2>Current Deployed Branch</h2>
      <Table striped bordered hover>
        <tbody>
          <tr>
            <td>GitHub Repo:</td>
            <td>
              <a href={systemInfo.sourceRepo}>{systemInfo.sourceRepo}</a>
            </td>
          </tr>
          <tr>
            <td>Commit Link:</td>
            <td>
              <a href={systemInfo.githubUrl}>{systemInfo.githubUrl}</a>
            </td>
          </tr>
          <tr>
            <td>Commit Hash:</td>
            <td>{systemInfo.commitId}</td>
          </tr>
          <tr>
            <td>Commit Message:</td>
            <td>{systemInfo.commitMessage}</td>
          </tr>
        </tbody>
      </Table>
      <blockquote>
        <p>
          Note: to ensure that the links to the currently deployed commit work
          properly, be sure to set the <code>SOURCE_REPO</code> property on your
          dokku deployment. For example:
        </p>
        <pre>
          {`dokku config:set my-deployment SOURCE_REPO=https://github.com/ucsb-cs156-s26/proj-frontiers-s26-17
`}
        </pre>
      </blockquote>
      <h2>Backend Endpoints</h2>
      <ul>
        <li>
          <a href="/swagger-ui/index.html">Swagger</a>
        </li>
      </ul>
      <h2>System Info</h2>
      <p>Click the arrow icons below to expand/collapse the tree.</p>
      <JsonView value={systemInfo} />
    </BasicLayout>
  );
};

export default DeveloperPage;
