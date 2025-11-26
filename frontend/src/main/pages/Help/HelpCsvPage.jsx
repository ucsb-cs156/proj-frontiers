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
                  <tr>
                    <td>Section</td>
                    <td>Enrl Cd</td>
                    <td>We keep the enrollment code for the Section column.</td>
                  </tr>
                </tbody>
              </table>
              <p className="text-muted">
                All other columns exported by eGrades (Grade, Units, Quarter,
                etc.) are ignored.
              </p>
            </Accordion.Body>
          </Accordion.Item>
          <Accordion.Item eventKey="1">
            <Accordion.Header>Chico State University</Accordion.Header>
            <Accordion.Body>
              <p>
                The following CSV format can be obtained from the Canvas system
                at Chico State, through the &quot;New Analytics&quot; feature:
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
                      We split the value at the last space; double-check
                      suffixes such as Jr. or III to ensure they end up on the
                      correct side.
                    </td>
                  </tr>
                  <tr>
                    <td>Student First</td>
                    <td>Student Name</td>
                    <td>
                      Everything before the final space (blank if no space is
                      present).
                    </td>
                  </tr>
                  <tr>
                    <td>Student ID</td>
                    <td>Student SIS ID</td>
                    <td>The “Student ID” column from Canvas is ignored.</td>
                  </tr>
                  <tr>
                    <td>Email</td>
                    <td>Email</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>Section</td>
                    <td>&mdash;</td>
                    <td>
                      Canvas includes a Section Name column, but Frontiers
                      ignores it and leaves the Section field blank for this
                      format.
                    </td>
                  </tr>
                </tbody>
              </table>
              <p className="text-muted">
                All other columns from the Canvas “New Analytics” export are
                ignored, so you can upload the file as-is; this includes Section
                Name, so roster sections remain blank after the upload.
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
                    <td>Sortable name</td>
                    <td>Everything before the comma.</td>
                  </tr>
                  <tr>
                    <td>Student First</td>
                    <td>Sortable name</td>
                    <td>Everything after the comma (or blank if none).</td>
                  </tr>
                  <tr>
                    <td>Student ID</td>
                    <td>SIS Id</td>
                    <td></td>
                  </tr>
                  <tr>
                    <td>Email</td>
                    <td>Email</td>
                    <td></td>
                  </tr>
                </tbody>
              </table>
              <p className="text-muted">
                Other analytics columns (grades, participation timestamps, etc.)
                are ignored when the CSV is processed, so the Section column in
                Frontiers remains blank for this format.
              </p>
            </Accordion.Body>
          </Accordion.Item>
        </Accordion>

        <h3 className="mt-4">How Frontiers handles dropped students</h3>
        <p>
          Each time you upload a roster CSV, Frontiers temporarily marks every
          roster student that originally came from a CSV (status{" "}
          <code>ROSTER</code>) as <code>DROPPED</code>. As rows from your new
          file are processed, matching students are switched back to{" "}
          <code>ROSTER</code> (or inserted if they were new).
        </p>
        <ul>
          <li>
            Only students that appeared in a prior CSV upload can move into the
            dropped section automatically.
          </li>
          <li>
            Students that you added manually keep their <code>MANUAL</code>{" "}
            status and are never moved to DROPPED by an upload.
          </li>
          <li>
            After the upload finishes, anyone still marked <code>DROPPED</code>{" "}
            is listed in the Dropped tab, and Frontiers queues them for removal
            from the linked GitHub organization.
          </li>
        </ul>
        <p>
          If a student disappears from your roster because their identifier
          changed, update the CSV before uploading so that they are not treated
          as dropped.
        </p>

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
              <p>
                The header row uses uppercase names and the columns appear in
                the exact order shown below.
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
                    <td>
                      <code>ID</code>
                    </td>
                    <td>Internal database ID</td>
                    <td>Unique identifier for each roster entry</td>
                  </tr>
                  <tr>
                    <td>
                      <code>COURSEID</code>
                    </td>
                    <td>Course database ID</td>
                    <td>Identifies which course this student is enrolled in</td>
                  </tr>
                  <tr>
                    <td>
                      <code>STUDENTID</code>
                    </td>
                    <td>Student ID</td>
                    <td>Student&apos;s university ID (e.g., Perm # at UCSB)</td>
                  </tr>
                  <tr>
                    <td>
                      <code>FIRSTNAME</code>
                    </td>
                    <td>Student&apos;s first name</td>
                    <td>Required when re-uploading this CSV.</td>
                  </tr>
                  <tr>
                    <td>
                      <code>LASTNAME</code>
                    </td>
                    <td>Student&apos;s last name</td>
                    <td>Required when re-uploading this CSV.</td>
                  </tr>
                  <tr>
                    <td>
                      <code>EMAIL</code>
                    </td>
                    <td>Student&apos;s email address</td>
                    <td>Required when re-uploading this CSV.</td>
                  </tr>
                  <tr>
                    <td>
                      <code>USERID</code>
                    </td>
                    <td>Internal user ID</td>
                    <td>Links to the user account in the system</td>
                  </tr>
                  <tr>
                    <td>
                      <code>GITHUBID</code>
                    </td>
                    <td>GitHub user ID</td>
                    <td>Student&apos;s GitHub account ID, if connected</td>
                  </tr>
                  <tr>
                    <td>
                      <code>GITHUBLOGIN</code>
                    </td>
                    <td>GitHub username</td>
                    <td>Student&apos;s GitHub username, if connected</td>
                  </tr>
                  <tr>
                    <td>
                      <code>ROSTERSTATUS</code>
                    </td>
                    <td>Roster enrollment status</td>
                    <td>
                      ROSTER (from uploaded roster), MANUAL (manually added), or
                      DROPPED
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <code>ORGSTATUS</code>
                    </td>
                    <td>GitHub organization status</td>
                    <td>PENDING, JOINCOURSE, INVITED, MEMBER, or OWNER</td>
                  </tr>
                  <tr>
                    <td>
                      <code>TEAMS</code>
                    </td>
                    <td>Team names</td>
                    <td>
                      A comma-separated list showing every team assigned to the
                      student.
                    </td>
                  </tr>
                </tbody>
              </table>
              <p>
                When you re-upload a file that originated from this download,
                Frontiers only reads the following columns; everything else is
                ignored, so you can upload the file without trimming it.
              </p>
              <ul>
                <li>
                  <code>EMAIL</code>
                </li>
                <li>
                  <code>FIRSTNAME</code>
                </li>
                <li>
                  <code>LASTNAME</code>
                </li>
                <li>
                  <code>STUDENTID</code>
                </li>
                <li>
                  <code>SECTION</code>
                </li>
              </ul>
            </Accordion.Body>
          </Accordion.Item>
        </Accordion>
      </div>
    </BasicLayout>
  );
}
