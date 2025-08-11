import React from "react";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { Accordion } from "react-bootstrap";

export default function HelpAboutPage() {
  return (
    <BasicLayout>
      <div className="pt-2">
        <h1 className="my-4">About Frontiers</h1>
        <p>This application has two main purposes</p>
        <ul>
          <li>
            Assist students and instructors in working with a GitHub
            organization associated with a course. In this role, it is similar
            to GitHub Classroom, but takes a somewhat different approach.
          </li>
          <li>
            Provide a platform for the Frontiers project, an NSF-sponsored
            project to develop metrics to asssess learning of fundamental
            Software Engineering Skills.
          </li>
        </ul>

        <h2 className="my-4">Features</h2>

        <p>
          More information is available about the features available for various
          types of users by clicking on the roles below
        </p>
        <Accordion>
          <Accordion.Item eventKey="0">
            <Accordion.Header>Instructors</Accordion.Header>
            <Accordion.Body>
              Instructors can do the following:
              <ul>
                <li>Create a course</li>
                <li>Link their course to a Github Organization</li>
                <li>
                  Upload a course roster from a CSV file, as well as add, update
                  and delete records for individual students.
                </li>
                <li>
                  Manage a roster of course staff (e.g. Teaching Assistants)
                </li>
                <li>
                  Provide a link for students on the course roster and staff
                  roster to automatically generate an invitation to join the
                  course organization{" "}
                </li>
                <li>
                  See the status of each student and staff member (i.e. whether
                  they have joined the organization yet or not), and see the
                  github ids of those that have
                </li>
                <li>
                  Download the course roster as a CSV that includes a column for
                  student Github ids
                </li>
                <li>
                  Upload information about teams and sync this with Github
                </li>
                <li>
                  Create repositories for course assignments for both
                  individuals and/or teams, either public or private, with any
                  level of access
                </li>
              </ul>
            </Accordion.Body>
          </Accordion.Item>
          <Accordion.Item eventKey="1">
            <Accordion.Header>Students</Accordion.Header>
            <Accordion.Body>
              <p>Students can do the following:</p>
              <ul>
                <li>Login with their University credentials</li>
                <li>
                  See a list of all courses that use Frontiers where they are
                  either a student or a staff member (e.g. a TA)
                </li>
                <li>
                  Automatically generate an invitation to the associated Github
                  organization, and join that organization
                </li>
              </ul>
            </Accordion.Body>
          </Accordion.Item>
          <Accordion.Item eventKey="2">
            <Accordion.Header>
              Future Features to support the Frontiers Project
            </Accordion.Header>
            <Accordion.Body>
              <p>
                Our development team is working on the following features to
                support the Frontiers project
              </p>
              <ul>
                <li>
                  Instructors will be able to define rubrics to evaluate four
                  types of GitHub artifacts: Commits, Issues, Pull Requests, and
                  Code Reviews
                </li>
                <li>
                  Instructors will be able to define collections of these
                  artifacts that are either fixed collections, or random samples
                  over defined criteria (such as repo(s), user(s), teams(s),
                  date ranges, etc.)
                </li>
                <li>
                  Instructors will be able to assign students and staff to
                  evaluate artifacts according to the rubrics.
                </li>
                <li>
                  Instructors will be able to calculate interrater reliability
                  using metrics such as Cohen's Kappa and Kronbach's Alpha to
                  determine whether students are applying the rubric in a way
                  that is consistent with experts.
                </li>
                <li>
                  Instructors will be able to see the results of these
                  evaluations, download them as CSV files, and use this in
                  assigning both grades for formative and summative assessment
                  of whether students have learned to produce Github artifacs of
                  high quality.
                </li>
              </ul>
            </Accordion.Body>
          </Accordion.Item>
        </Accordion>
        <h2 className="my-4">NSF Awards</h2>
        <p>
          Frontiers is supported by the National Science Foundation (NSF) under
          the following awards:
        </p>

        <table className="table table-striped">
          <thead>
            <tr>
              <th>Principal Investigator</th>
              <th>Institution</th>
              <th>NSF Award Number</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Chris Hundhausen (Lead)</td>
              <td>Oregon State University</td>
              <td>
                <a href="https://www.nsf.gov/awardsearch/showAward?AWD_ID=2337269&HistoricalAwards=false">
                  2337269
                </a>
              </td>
            </tr>
            <tr>
              <td>Sola Adesope</td>
              <td>Washington State University</td>
              <td>
                <a href="https://www.nsf.gov/awardsearch/showAward?AWD_ID=2337272&HistoricalAwards=false">
                  2337272
                </a>
              </td>
            </tr>
            <tr>
              <td>Kevin Buffardi</td>
              <td>Chico State University</td>
              <td>
                <a href="https://www.nsf.gov/awardsearch/showAward?AWD_ID=2337271&HistoricalAwards=false">
                  2337271
                </a>
              </td>
            </tr>
            <tr>
              <td>Phillip Conrad</td>
              <td>University of California, Santa Barbara</td>
              <td>
                <a href="https://www.nsf.gov/awardsearch/showAward?AWD_ID=2337270&HistoricalAwards=false">
                  2337270
                </a>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </BasicLayout>
  );
}
