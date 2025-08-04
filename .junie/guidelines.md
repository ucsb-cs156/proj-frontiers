# Process Notes

When making changes to the codebase, ensure that you:
- Add unit tests under the frontend/src/tests when working on the frontend and src/test when working on the backend
- Ensure that any added tests pass before completing.
- When working in the frontend, ensure you run `npm run format` just prior to completing your work. This command must be run from the `frontend/` directory. 

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
