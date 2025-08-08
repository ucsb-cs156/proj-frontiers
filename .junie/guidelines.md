# Process Notes

When making changes to the codebase, ensure that you:
- Add unit tests under the frontend/src/tests when working on the frontend and src/test when working on the backend
- Ensure that any added tests pass before completing.
- Also ensure that your changes have complete line coverage.
- When working in the frontend, ensure you run `npm run format` just prior to completing your work. This command must be run from the `frontend/` directory.
- When possible, verify if the changes are present by starting the front and backend.

Note that you should look for the simplest solution and attempt to avoid adding any dependencies.

Please avoid leaving comments where the code is self explanatory: only add comments that may help illuminate what is occurring in difficult code chunks.

If `<issue_description>` directly contradicts any of these steps, follow the instructions from `<issue_description>` first.

# Information
To start the backend of the project, you can run `mvn spring-boot:run`. In the event the port is taken, you can run `PORT=<port> mvn spring-boot:run` to set the port.

To start the frontend, you can run `BROWSER=none npm start`.

Line Coverage can be checked on the backend within `mvn test jacoco:report`

The testing suite in the backend can be run with `mvn test`.

The testing suite in the frontend can be run in the `frontend/` directory with `npm test -- --watchAll=false`

Line Coverage for the frontend can be checked in the `frontend/` directory with `npm run coverage`.

# Backend vs Frontend

When an issue requests that you add a new backend endpoint, and says nothing about the frontend, please do as the issue says, and ONLY implement the backend endpoint.

We use swagger to document and test our API endpoints in separate PRs before we proceed to implement a frontend on top of them.  This enables us to make faster progress and keeps each PR focused and simple.

Of course, if a PR requests a *change* to an *existing backend API endpoint, and that would break existing frontend functionality, then by all means, implement the necessary changes to the frontend in the same PR.  But do not take it upon your own initative to add frontend code for a new backend endpoint if the issue said nothing about doing that.  Just implement the new backend endpoint, and nothing else.

Similarly, there may be times when you are asked to implement a new frontend component, along with tests, and stories for the Storybook tool.  We do this enable us to prototype user interfaces before we have completed the backend. In these cases, we use fixtures and the msw tool to mock the backend that we expect to build.   In these cases, just do what the issues asks.  If it says frontend only, that's what it means. Don't build the backend too, unless you are specifically asked to do so.

