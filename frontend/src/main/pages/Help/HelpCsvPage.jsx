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
          formats are supported. If you would like your university&apos;s format
          to be supported, please contact the Frontiers development team,
          supplying a sample CSV (with fictional data).
        </p>
        <h3>How &quot;Dropped&quot; Students Are Handled</h3>
        <p>
          Only students that appeared in an uploaded CSV and then did NOT appear
          in a subsequent CSV upload are considered dropped; manually added
          students are never put into the dropped section
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
                    <td>Student First Middle</td>
                    <td>Student First Name</td>
                    <td>We only keep the First Name</td>
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
                  <tr>
                    <td>Section</td>
                    <td>Enrl Cd</td>
                    <td>Section is kept as an enrollment code</td>
                  </tr>
                </tbody>
              </table>
              <p className="text-muted">
                All other columns obtained from the EGradessystem at UCSB are
                ignored.
              </p>
            </Accordion.Body>
          </Accordion.Item>
          <Accordion.Item eventKey="1">
            <Accordion.Header>Chico State University</Accordion.Header>
            <Accordion.Body>
              <p>
                The following CSV format can be obtained from the Canvas system
                at Chico State, through the New &quot;Analytics&quot; feature:
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
                    <td>Student Last</td>
                    <td>Student Name</td>
                    <td>
                      We take everything after the last space. Instructors may
                      need to review to ensure correct names
                    </td>
                  </tr>
                  <tr>
                    <td>Student First</td>
                    <td>Student Name</td>
                    <td>
                      We take everything before the last space. Instructors may
                      need to review to ensure correct names
                    </td>
                  </tr>
                  <tr>
                    <td>Student ID</td>
                    <td>Student SIS ID</td>
                    <td>
                      Student SIS ID is taken instead of Student ID from the CSV
                    </td>
                  </tr>
                  <tr>
                    <td>Email</td>
                    <td>Email</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>Section</td>
                    <td>Ignored</td>
                    <td>Left Blank</td>
                  </tr>
                </tbody>
              </table>
              <p className="text-muted">
                All other columns obtained from the Canvas System at Chico State
                are ignored.
              </p>
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
                    <td>Student Last</td>
                    <td>Sortable Name</td>
                    <td>We take everything after the comma</td>
                  </tr>
                  <tr>
                    <td>Student First</td>
                    <td>Sortable Name</td>
                    <td>We take everything before the comma</td>
                  </tr>
                  <tr>
                    <td>Student ID</td>
                    <td>SIS ID</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>Email</td>
                    <td>Email</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>Section</td>
                    <td>N/A</td>
                    <td>Not provided from CSV</td>
                  </tr>
                </tbody>
              </table>
              <p className="text-muted">
                All other columns obtained from Orgeon State University are
                ignored.
              </p>
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
                      Student&apos;s email address used to match to the roster.
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
                    <td>ID</td>
                    <td>Internal database ID</td>
                    <td>Unique identifier for each roster entry</td>
                  </tr>
                  <tr>
                    <td>COURSEID</td>
                    <td>Course database ID</td>
                    <td>Identifies which course this student is enrolled in</td>
                  </tr>
                  <tr>
                    <td>STUDENTID</td>
                    <td>Student ID</td>
                    <td>Student&apos;s university ID (e.g., Perm # at UCSB)</td>
                  </tr>
                  <tr>
                    <td>FIRSTNAME</td>
                    <td>Student&apos;s first name</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>LASTNAME</td>
                    <td>Student&apos;s last name</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>EMAIL</td>
                    <td>Student&apos;s email address</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>SECTION</td>
                    <td>Student&apos;s section time</td>
                    <td>Date, Time, and Location of Section</td>
                  </tr>
                  <tr>
                    <td>USERID</td>
                    <td>Internal user ID</td>
                    <td>Links to the user account in the system</td>
                  </tr>
                  <tr>
                    <td>GITHUBID</td>
                    <td>GitHub user ID</td>
                    <td>Student&apos;s GitHub account ID, if connected</td>
                  </tr>
                  <tr>
                    <td>GITHUBLOGIN</td>
                    <td>GitHub username</td>
                    <td>Student&apos;s GitHub username, if connected</td>
                  </tr>
                  <tr>
                    <td>ROSTERSTATUS</td>
                    <td>Roster enrollment status</td>
                    <td>
                      ROSTER (from uploaded roster), MANUAL (manually added), or
                      DROPPED
                    </td>
                  </tr>
                  <tr>
                    <td>ORGSTATUS</td>
                    <td>GitHub organization status</td>
                    <td>PENDING, JOINCOURSE, INVITED, MEMBER, or OWNER</td>
                  </tr>
                  <tr>
                    <td>TEAMS</td>
                    <td>Student&apos;s Team(s)</td>
                    <td>Name of Team(s)</td>
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
