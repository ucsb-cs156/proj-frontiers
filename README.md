# STARTER-team01

Instructions: <https://ucsb-cs156.github.io/f24/lab/team01.html>

TODO: change heading above to your repo name, e.g. `# team01-f24-17`

TODO: Add a link to the deployed Dokku app for your team here, e.g.

Deployments:

* Prod: <https://team01.dokku-17.cs.ucsb.edu>
* QA: <https://team01-qa.dokku-17.cs.ucsb.edu>

TODO: Fill in this table with correct information. 

| Table                     | Name         | Github Id |
|---------------------------|--------------|-----------|
| UCSBDiningCommonsMenuItem |              |           |
| UCSBOrganization          |              |           |
| RecommendationRequest     |              |           |
| MenuItemReview            |              |           |
| HelpRequest               |              |           |
| Articles                  |              |           |

Remember though, that in spite of these initial  assignments, it is still
a team project.  Please help other team members to finish their work
after completing your own.

# Versions
* Java: 21
* node: 20.17.0
See [docs/versions.md](docs/versions.md) for more information on upgrading versions.

# Brief overview of starter code 

TODO: remove this header and content of this section before submitting.
However leave the section `# Overview of application` and its content 
intact.

The starter code here starts with a base similar to `team01`, but with 
some extra frontend code on top of the of backend CRUD operations
that were present in `team01`.

You can use this code as a basis to:
* Add the backend code from team01 *in stages* as suggested in the issues (doing that in "one giant pull request" is *not recommended) 
* Add a frontend on top of the backend CRUD features you added in team01, using the existing
  code as examples.

# Overview of application

When complete, this application will have the following features:

* For users that are not logged in, no new features are available.
* For users that are logged in, they will see a new menu dropdown called Tables,
  and under it will be menu items for each of the tables that was implemented in the 
  application (e.g. some subset of `UCSBDiningCommonsMenuItem`, `UCSBOrganization` `RecommendationRequest`,`MenuItemReview`,`HelpRequest`, and `Articles`).
* For users that are logged in, but are not admins, each of these menu items takes them to
  an "index" page for that database table, where they see a list of all of the database
  records for that table in table form.  The index page, in this case, will not have a button
  to create database records, and it will not have buttons for Edit and Delete; only Show.
* The Show button will take the user to a page where they see only one record from the database
  table.
* For users that are logged in as admins, they will also see a button on the index page that
  takes them to a page where they can create a new record in the database.  That page, when the
  user successfully creates a record, or cancels creating a record, 
  should navigate back to the index page.
* For users that are logged in as admins, in addition to the Show button, 
  they will also see a button on the index page that
  beside each row for Edit, and Delete.
* The Edit button, for admins, will navigate to a page where the database record can be edited.
  After a successful edit, the page will navigate back to the index page.
* The Delete button, for admins, will make the api call to delete the row, and then 
  navigate back to the index page.


# Setup before running application

Before running the application for the first time,
you need to do the steps documented in [`docs/oauth.md`](docs/oauth.md).

Otherwise, when you try to login for the first time, you 
will likely see an error such as:

<img src="https://user-images.githubusercontent.com/1119017/149858436-c9baa238-a4f7-4c52-b995-0ed8bee97487.png" alt="Authorization Error; Error 401: invalid_client; The OAuth client was not found." width="400"/>

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

By convention, we are putting Integration tests (the ones that run with Playwright) under the package `src/test/java/edu/ucsb/cs156/example/web`.

Unless you want a particular integration test to *also* be run when you type `mvn test`, do *not* use the suffixes `Test` or `Tests` for the filename.

Note that while `mvn test` is typically sufficient to run tests, we have found that if you haven't compiled the test code yet, running `mvn failsafe:integration-test` may not actually run any of the tests.


## Partial pitest runs

This repo has support for partial pitest runs

For example, to run pitest on just one class, use:

```
mvn pitest:mutationCoverage -DtargetClasses=edu.ucsb.cs156.example.controllers.RestaurantsController
```

To run pitest on just one package, use:

```
mvn pitest:mutationCoverage -DtargetClasses=edu.ucsb.cs156.example.controllers.\*
```

To run full mutation test coverage, as usual, use:

```
mvn pitest:mutationCoverage
```