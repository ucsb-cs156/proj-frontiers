# proj-frontiers

* Prod: <https://frontiers.dokku-00.cs.ucsb.edu>
* QA: <https://frontiers-qa.dokku-00.cs.ucsb.edu>

### Product Planning Doc: 
- <https://docs.google.com/document/d/1IXxmD4hBvDq6uSnpSukdV30o4xyaIAgiRt_Mpqv51yw/edit?usp=sharing>

# Versions
* Java: 21
* node: 20.17.0
See [docs/versions.md](docs/versions.md) for more information on upgrading versions.

# Overview of application

This web application supports courses that use Github organizations
as a basis for instruction in coding and software engineering.

Three levels of functionalilty are planned:

Tier 1: Basic features to automate the process of generating invitations to a Github Organization for all students on a course roster, and linking those students Github and student identities (i.e. linking their Github login to their official school email or student number).

Tier 2: Features to support common course management tasks such as creating assignment repos for individuals or groups, managing teams on Github, creating team level repos.

Tier 3: Features to support the NSF Frontiers project, a project that aims to provide instructors
in Software Engineering courses with tools to help students learn how to evaluate commits,
issues, pull requests and code reviews using rubrics based on criteria established by an expert panel of representatives from both industry and academia.

As of April 2025, we have only a minimum viable product for Tier 1 functionality.  The goal is to complete Tier 1 as soon as possible, and and then start building Tier 2 and Tier 3 features.

# Setup before running application

Before running the application for the first time,
you need to do the steps documented in [`docs/oauth.md`](docs/oauth.md).

Otherwise, when you try to login for the first time, you 
will likely see an error such as:

<img src="https://user-images.githubusercontent.com/1119017/149858436-c9baa238-a4f7-4c52-b995-0ed8bee97487.png" alt="Authorization Error; Error 401: invalid_client; The OAuth client was not found." width="400"/>

For certain functions to work properly, you'll also need to set up the app as a Github App.  Here's how:

* On localhost: [`docs/github-app-setup-localhost.md`](docs/github-app-setup-localhost.md)
* On dokku: [`docs/github-app-setup-localhost.md`](docs/github-app-setup-localhost.md)

# Getting Started on localhost

* Open *two separate terminal windows*  
* In the first window, start up the backend with:
  ``` 
  mvn spring-boot:run
  ```
* In the second window:
  ```
  cd frontend
  npm ci  # only on first run
  npm start
  ```

Then, the app should be available on <http://localhost:8080>

If it doesn't work at first, e.g. you have a blank page on  <http://localhost:8080>, give it a minute and a few page refreshes.  Sometimes it takes a moment for everything to settle in.

If you see the following on localhost, make sure that you also have the frontend code running in a separate window.

```
Failed to connect to the frontend server... On Dokku, be sure that PRODUCTION is defined.  On localhost, open a second terminal window, cd into frontend and type: npm install; npm start";
```

# Getting Started on Dokku

See: [/docs/dokku.md](/docs/dokku.md)

# Accessing swagger

To access the swagger API endpoints, use:

* <http://localhost:8080/swagger-ui/index.html>

Or add `/swagger-ui/index.html` to the URL of your dokku deployment.

# To run React Storybook

* cd into frontend
* use: npm run storybook
* This should put the storybook on http://localhost:6006
* Additional stories are added under frontend/src/stories

For documentation on React Storybook, see: 
* <https://ucsb-cs156.github.io/topics/storybook/>
* <https://ucsb-cs156.github.io/topics/chromatic/>
* <https://storybook.js.org/>

# SQL Database access

On localhost:
* The SQL database is an H2 database and the data is stored in a file under `target`
* Each time you do `mvn clean` the database is completely rebuilt from scratch
* You can access the database console via a special route, <http://localhost:8080/h2-console>
* For more info, see [docs/h2-database.md](/docs/h2-database.md)

On Dokku, follow instructions for Dokku databases:
* <https://ucsb-cs156.github.io/topics/dokku/postgres_database.html>

# Testing

## Unit Tests

* To run all unit tests, use: `mvn test`
* To run only the tests from `FooTests.java` use: `mvn test -Dtest=FooTests`

Unit tests are any methods labelled with the `@Test` annotation that are under the `/src/test/java` hierarchy, and have file names that end in `Test` or `Tests`

## Integration Tests

To run only the integration tests, use:
```
INTEGRATION=true mvn test-compile failsafe:integration-test
```

To run only the integration tests *and* see the tests run as you run them,
use:

```
INTEGRATION=true HEADLESS=false mvn test-compile failsafe:integration-test
```

To run a particular integration test (e.g. only `HomePageWebIT.java`) use `-Dit.test=ClassName`, for example:

```
INTEGRATION=true mvn test-compile failsafe:integration-test -Dit.test=HomePageWebIT
```

or to see it run live:
```
INTEGRATION=true HEADLESS=false mvn test-compile failsafe:integration-test -Dit.test=HomePageWebIT
```

Integration tests are any methods labelled with `@Test` annotation, that are under the `/src/test/java` hierarchy, and have names starting with `IT` (specifically capital I, capital T).

By convention, we are putting Integration tests (the ones that run with Playwright) under the package `src/test/java/edu/ucsb/cs156/frontiers/web`.

Unless you want a particular integration test to *also* be run when you type `mvn test`, do *not* use the suffixes `Test` or `Tests` for the filename.

Note that while `mvn test` is typically sufficient to run tests, we have found that if you haven't compiled the test code yet, running `mvn failsafe:integration-test` may not actually run any of the tests.


## Partial pitest runs

This repo has support for partial pitest runs

For example, to run pitest on just one class, use:

```
mvn pitest:mutationCoverage -DtargetClasses=edu.ucsb.cs156.frontiers.controllers.CoursesController
```

To run pitest on just one package, use:

```
mvn pitest:mutationCoverage -DtargetClasses=edu.ucsb.cs156.frontiers.controllers.\*
```

To run full mutation test coverage, as usual, use:

```
mvn pitest:mutationCoverage
```
