import React from "react";
import { Accordion } from "react-bootstrap";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import csvFixtures from "fixtures/csvFixtures";

export default function HelpCsvPage() {
  return (
    <BasicLayout>
      <div className="pt-2">
        <h1 className="my-4">CSV Upload/Download Formats</h1>
        <h2>Roster Student CSV Upload formats</h2>
        <p>
          Instructors can upload a course roster from a CSV file. The following
          formats are supported. If you would like your university's format to
          be supported, please contact the Frontiers development team, supplying
          a sample CSV (with fictional data).
        </p>
        <Accordion>
          <Accordion.Item eventKey="0">
            <Accordion.Header>Chico State University</Accordion.Header>
            <Accordion.Body>
              <p>
                The following CSV format can be obtained from the Canvas system
                at Chico State, through the "New Analytics" feature:
              </p>
              <pre className={"csvExample"} data-testid="chicoStateCsvExample">
                {csvFixtures.chicoStateCanvas}
              </pre>
              <table className="table table-striped">
                <thead>
                  <tr>
                    <th>Roster Field</th>
                    <th>CSV Field</th>
                    <th>Notes</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Student Name</td>
                    <td>Student Last Name</td>
                    <td>
                      We split the name at the last space; instructors may have
                      to correct this for some students.
                    </td>
                  </tr>
                  <tr>
                    <td>Student Name</td>
                    <td>Student First Name</td>
                    <td>See above.</td>
                  </tr>
                  <tr>
                    <td>Student SIS ID</td>
                    <td>Student ID</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>Email</td>
                    <td>Email</td>
                    <td></td>
                  </tr>
                </tbody>
              </table>
            </Accordion.Body>
          </Accordion.Item>
          <Accordion.Item eventKey="1">
            <Accordion.Header>UC Santa Barbara</Accordion.Header>
            <Accordion.Body>
              <p>
                The following CSV format can be obtained from the EGradessystem
                at UC Santa Barbara:
              </p>
              <pre className={"csvExample"} data-testid="ucsbEgradesCsvExample">
                {csvFixtures.ucsbEgrades}
              </pre>
              <p>We map the fields as follows:</p>
              <table className="table table-striped">
                <thead>
                  <tr>
                    <th>Roster Field</th>
                    <th>CSV Field</th>
                    <th>Notes</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Student Last</td>
                    <td>Student Last Name</td>
                  </tr>
                  <tr>
                    <td>Student First</td>
                    <td>Student First Name</td>
                  </tr>
                  <tr>
                    <td>Perm #</td>
                    <td>Student ID</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>Email</td>
                    <td>Email</td>
                    <td>
                      We translate all <code> @umail.ucsb.edu </code> addresses
                      to <code> @ucsb.edu</code>
                    </td>
                  </tr>
                </tbody>
              </table>
            </Accordion.Body>
          </Accordion.Item>
        </Accordion>
      </div>
    </BasicLayout>
  );
}
