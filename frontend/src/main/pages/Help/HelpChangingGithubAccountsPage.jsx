import React from "react";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";

export default function HelpChangingGithubAccountsPage() {
  return (
    <BasicLayout>
      <div className="pt-2">
        <h1 className="my-4">Changing Github Accounts</h1>
        <p>
          It often happens that during the initial on-boarding, before the
          course has really gotten started, a student realizes that they signed
          up with a different Github Account than the one they intended to use
          in the course.
        </p>
        <p>
          In the student list on the instructor/staff view, if the student
          account shows the wrong github in the Github Login column (i.e. the
          one the student doesn&apos;t want to use), here&apos;s what to do:
        </p>
        <ol>
          <li>
            The instructor or a staff member should delete the student by
            clicking the &quot;Delete&quot; button in the student table in the
            instructor/staff Student tab.
          </li>
          <li>
            The instructor should then re-add the student by re-uploading the
            course roster, syncing from Canvas, or manually adding the student.
          </li>
          <li>
            The student should then log in to Frontiers again, but{" "}
            <i>should not immediately join the course</i>. Instead, they should
            click on the &quot;Welcome, email@school.edu&quot; link, upper right
            of the menu bar. On the page this takes the student to, they should
            click the &quot;Disconnect Github&quot; button. They should then
            login to Github again with the preferred Github Account.
          </li>
          <li>
            Then, they should rejoin the course and accept the invitation to the
            github org.
          </li>
          <li>
            Finally, they should contact the instructor to let them know; the
            instructor may need to refresh some assignments to ensure that they
            are created for the students new Github Account.
          </li>
        </ol>
      </div>
    </BasicLayout>
  );
}
