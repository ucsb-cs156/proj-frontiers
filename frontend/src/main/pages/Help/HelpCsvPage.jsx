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
        <Accordion data-testid="rosterUploadsAccordion">
          <Accordion.Item eventKey="0">
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
                    <td></td>
                  </tr>
                  <tr>
                    <td>Student First</td>
                    <td>Student First Name</td>
                    <td></td>
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
          <Accordion.Item eventKey="1">
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
          <Accordion.Item eventKey="2">
            <Accordion.Header>Oregon State University</Accordion.Header>
            <Accordion.Body>
              <p>
                The following CSV format can be obtained for courses at Oregon
                State University.
              </p>
              <pre className={"csvExample"} data-testid="oregonStateCsvExample">
                {csvFixtures.oregonStateCSV}
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
                    <td>Sortable Name</td>
                    <td>Student Last Name</td>
                    <td>We split the name at the comma.</td>
                  </tr>
                  <tr>
                    <td>Sortable Name</td>
                    <td>Student First Name</td>
                    <td>See above.</td>
                  </tr>
                  <tr>
                    <td>SIS ID</td>
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
        </Accordion>

        <h2 className="mt-4">Team Information</h2>
        <p>
          Teams can be managed using a simple CSV upload with the following
          format.
        </p>
        <Accordion data-testid="teamsAccordion">
          <Accordion.Item eventKey="0">
            <Accordion.Header>Teams (by Email)</Accordion.Header>
            <Accordion.Body>
              <p>
                You can upload teams using a simple CSV with the following
                format:
              </p>
              <pre className={"csvExample"} data-testid="teamsCsvExample">
                {csvFixtures.teamsByEmail}
              </pre>
              <p>We interpret the fields as follows:</p>
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
                    <td>Team Name</td>
                    <td>team</td>
                    <td>
                      Rows with the same team value belong to the same team.
                    </td>
                  </tr>
                  <tr>
                    <td>Email</td>
                    <td>email</td>
                    <td>
                      Student's email address used to match to the roster.
                    </td>
                  </tr>
                </tbody>
              </table>
            </Accordion.Body>
          </Accordion.Item>
        </Accordion>

        <h2 className="mt-4">Roster Student CSV Download formats</h2>
        <p>
          Instructors can download a course roster as a CSV file. The download
          will include all roster students with their current status
          information.
        </p>
        <Accordion>
          <Accordion.Item eventKey="0">
            <Accordion.Header>Frontiers CSV Download Format</Accordion.Header>
            <Accordion.Body>
              <p>
                When you download a roster as CSV, you will get the following
                format:
              </p>
              <pre
                className={"csvExample"}
                data-testid="rosterDownloadCsvExample"
              >
                {csvFixtures.rosterDownload}
              </pre>
              <p>The fields are described as follows:</p>
              <table className="table table-striped">
                <thead>
                  <tr>
                    <th>CSV Field</th>
                    <th>Description</th>
                    <th>Notes</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>id</td>
                    <td>Internal database ID</td>
                    <td>Unique identifier for each roster entry</td>
                  </tr>
                  <tr>
                    <td>courseId</td>
                    <td>Course database ID</td>
                    <td>Identifies which course this student is enrolled in</td>
                  </tr>
                  <tr>
                    <td>studentId</td>
                    <td>Student ID</td>
                    <td>Student's university ID (e.g., Perm # at UCSB)</td>
                  </tr>
                  <tr>
                    <td>firstName</td>
                    <td>Student's first name</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>lastName</td>
                    <td>Student's last name</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>email</td>
                    <td>Student's email address</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>section</td>
                    <td>Course section name/time</td>
                    <td>Identifies which section of the course this student is enrolled in</td>
                  </tr>
                  <tr>
                    <td>userId</td>
                    <td>Internal user ID</td>
                    <td>Links to the user account in the system</td>
                  </tr>
                  <tr>
                    <td>githubId</td>
                    <td>GitHub user ID</td>
                    <td>Student's GitHub account ID, if connected</td>
                  </tr>
                  <tr>
                    <td>githubLogin</td>
                    <td>GitHub username</td>
                    <td>Student's GitHub username, if connected</td>
                  </tr>
                  <tr>
                    <td>rosterStatus</td>
                    <td>Roster enrollment status</td>
                    <td>
                      ROSTER (from uploaded roster), MANUAL (manually added), or
                      DROPPED
                    </td>
                  </tr>
                  <tr>
                    <td>orgStatus</td>
                    <td>GitHub organization status</td>
                    <td>PENDING, JOINCOURSE, INVITED, MEMBER, or OWNER</td>
                  </tr>
                  <tr>
                    <td>teams</td>
                    <td>List of team names</td>
                    <td>Identifies which team(s) the student is a part of, if any</td>
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
