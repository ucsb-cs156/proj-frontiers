This folder contains examples of files used as input to proj-frontiers

## Rosters

* `OSU_CSV.csv` is an example (with fake data) of a roster for a course at Oregon State University in the format downloadable from Canvas at OSU. Student ids are in the `SIS Id` column and emails are `@oregonstate.edu` addresses.
* `chico_canvas.csv` is an example (with fake data) of a roster for a course at Chico State University in the format downloadable from Canvas at Chico State.
* `egrades.csv` is an example (with fake data) of a roster for a course at UCSB, in the format provided to instructors on the `eGrades` web app.
* `egrades_one_student_dropped.csv` is the same roster as `egrades.csv` but with one student (`ldelplaya@umail.ucsb.edu`) removed. Upload `egrades.csv` first, then this file, to test how the app handles a student who has dropped the course.
* `egrades_with_duplicate_emails.csv` is the `egrades.csv` roster plus an extra row that reuses an existing email (`sabadotarde@umail.ucsb.edu`) with a different Perm #. Used to test how the app handles two roster rows that share an email address.
* `egrades_with_duplicate_ids.csv` is the `egrades.csv` roster plus an extra row that reuses an existing Perm # (`1234567`) with a different name and email. Used to test how the app handles two roster rows that share a student id.
* `egrades_with_email_and_student_ids_from_diff_students.csv` is a roster in which the third row combines one student's Perm # (`A123456`, Chris Gaucho's) with another student's email (`ldelplaya@umail.ucsb.edu`, Lauren Del Playa's). Used to test how the app handles a row whose id and email each already belong to different existing students.

## Staff

* `staff_example.csv` is an example of a csv file used to upload course staff. It has the columns `firstName`, `lastName`, and `email`.

## Teams

* `email_csv_example.csv` is an example of a csv file used to create teams. It is intended to be used after `egrades.csv` has been loaded. It will reject one user: `bob@ucsb.edu`, who does not appear on the roster.
* `teams/team_data_with_missing_values.csv` is a team upload file (columns `team`, `email`) in which one row has a blank team name. Used to test how the app handles missing values in a team upload.

## CATME

* `CATME/` contains a matched set of fictitious CATME, eGrades and team CSV files for testing the CATME integration (see [`CATME/README.md`](CATME/README.md)).
