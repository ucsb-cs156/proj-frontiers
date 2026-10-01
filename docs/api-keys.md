# Frontiers Course-Scoped API Keys

Frontiers can issue API keys that let a script (a Gradescope autograder, a GitHub Action, a shell
script on an instructor's laptop) call a few endpoints without a logged-in user. Each key:

* belongs to **one course**, and only works for requests about that course (a key for course 2
  cannot read course 3);
* expires 90 days or 6 months after it is created;
* can be revoked at any time;
* only works on the endpoints listed under [Eligible endpoints](#eligible-endpoints) below. Any
  other endpoint answers `403` with the message
  `Attempting to gain access to an API endpoint that does not allow an API key`.

Frontiers never stores the key itself, only a hash of it, so a key is shown exactly once, when it is
created. If you lose it, revoke it and create a new one.

## Enabling API keys for a course

API keys are off by default. An instructor turns them on per course with the **Enable Api Keys**
switch on the **Settings** tab of the course page. While the switch is off:

* creating a key fails with `400` and the message
  `The course option ENABLE_API_KEYS must be enabled to create an API key.`;
* every existing key of the course is refused with `403` and the message
  `API keys are not enabled for this course`.

So turning the switch off is a one-click way to disable all API access to a course; turning it back
on brings the (unexpired, unrevoked) keys back to life.

## Creating a key

On the course page, open the **API Keys** tab (it appears once **Enable Api Keys** is on) and click
**Create API Key**. Give the key an optional label (at most 60 characters), such as which autograder
or GitHub Action will use it, choose whether it expires after 90 days or 6 months, and submit.

The key is shown **once**, in a dialog with a **Copy** button and a ready-to-paste example request.
Copy it somewhere safe (a secret in your CI system, a file inside your autograder zip that is not
committed to a public repo). Once the dialog is closed the key cannot be retrieved again; if it is
lost, revoke it and create a new one.

The same thing can be done from Swagger (`/swagger-ui/index.html`, section **Course API Key**):

```
POST /api/courses/key?courseId=2&choice=DAYS_90&label=jpa02%20autograder%20F26
```

```json
{ "key": "u1Zc6N0T0dG9xWcY4h2JbQ", "courseId": 2, "issuedAt": "...", "expiresAt": "...", "label": "jpa02 autograder F26" }
```

## Seeing which keys exist

The table on the **API Keys** tab lists every key ever created for the course, newest first:

| Column | Meaning |
|---|---|
| Label | The name given when the key was created, if any |
| Key | The last six characters of the key, enough to match it with the copy you saved |
| Created by, Created | Who created the key, and when |
| Expires | When the key stops working. An active key within 14 days of expiry gets an **expires in N days** warning |
| Last used, Uses | When the key was last used and how many requests it has made; a quick way to confirm an autograder is really using it |
| Status | **ACTIVE**, **EXPIRED** or **REVOKED** |

The same list is available from Swagger as `GET /api/courses/key?courseId=2`:

```json
[
  {
    "id": 7,
    "label": "jpa02 autograder F26",
    "keySuffix": "4h2JbQ",
    "createdByEmail": "phtcon@ucsb.edu",
    "createdAt": "2026-09-28T10:15:00-07:00",
    "expiresAt": "2026-12-27T10:15:00-08:00",
    "lastUsedAt": "2026-10-01T08:02:11-07:00",
    "usageCount": 143,
    "revoked": false,
    "status": "ACTIVE"
  }
]
```

## Using a key

Send the key in the `X-API-KEY` header. No cookie, CSRF token or login is needed.

```bash
curl -s 'https://frontiers.dokku-00.cs.ucsb.edu/api/courses/emails?courseId=2' \
  -H 'X-API-KEY: u1Zc6N0T0dG9xWcY4h2JbQ'
```

Possible errors, all as JSON with a `message` field:

| Status | Message | Meaning |
|---|---|---|
| 403 | `Invalid API key` | No such key (typo, or it was created on another Frontiers instance) |
| 403 | `API key has been revoked` | Someone revoked it; create a new one |
| 403 | `API key has expired` | Create a new one |
| 403 | `API keys are not enabled for this course` | Turn **Enable Api Keys** on in the course Settings |
| 403 | `Access Denied` | The key is for a different course than `courseId` |
| 403 | `Attempting to gain access to an API endpoint that does not allow an API key` | That endpoint is not in the table below |

## Eligible endpoints

| Endpoint | Purpose |
|---|---|
| `GET /api/courses/emails` | Email addresses of the students and/or staff of the course |
| `GET /api/courses/studentInfo` | One student's GitHub login, team and teammates, for autograders |

Endpoints are opted in one by one with the `@AllowApiKeyAccess` annotation in the backend; nothing
else accepts a key.

### `GET /api/courses/emails`

Parameters: `courseId` (required), `type` (`STUDENTS`, `STAFF` or `ALL`; default `STUDENTS`),
`team` (optional team name; limits the students to that team), `format` (`ONE_PER_LINE` or
`COMMA_SEPARATED`; default `ONE_PER_LINE`). Dropped students are never included.

```bash
COURSE_EMAILS="$(
curl -s -X 'GET' \
  'https://frontiers.dokku-00.cs.ucsb.edu/api/courses/emails?courseId=2&type=STUDENTS&format=ONE_PER_LINE' \
  -H 'X-API-KEY: u1Zc6N0T0dG9xWcY4h2JbQ'
)"
for email in $COURSE_EMAILS; do
  echo "student has email: $email"
done
```

For a course with students `ldelplaya@ucsb.edu` and `cgaucho@ucsb.edu`, this prints:

```
student has email: ldelplaya@ucsb.edu
student has email: cgaucho@ucsb.edu
```

### `GET /api/courses/studentInfo`

Looks up one student of the course by email and returns what an autograder typically needs to check
a submission against the roster. Parameters: `courseId` (required) and `email` (required).

* The email match is case-insensitive, and `@umail.ucsb.edu` is treated as `@ucsb.edu`, so the
  address Gradescope reports for the submitter can be passed through as is.
* Dropped students are not found, and are not listed as anyone's teammate.
* If no student matches, the response is `404` with a message such as
  `No roster student with email cgaucho@ucsb.edu in course 2`.

```bash
curl -s 'https://frontiers.dokku-00.cs.ucsb.edu/api/courses/studentInfo?courseId=2&email=CGaucho@umail.ucsb.edu' \
  -H 'X-API-KEY: u1Zc6N0T0dG9xWcY4h2JbQ'
```

```json
{
  "email": "cgaucho@ucsb.edu",
  "firstName": "Chris G",
  "legalFirstName": "CHRIS EDWARD",
  "lastName": "GAUCHO",
  "githubLogin": "cgaucho",
  "team": "f26-04",
  "teams": ["f26-04"],
  "teamMembers": [
    { "email": "cgaucho@ucsb.edu",   "firstName": "Chris G", "legalFirstName": "CHRIS EDWARD", "githubLogin": "cgaucho" },
    { "email": "clee@ucsb.edu",      "firstName": "Chris L", "legalFirstName": "CHRIS",        "githubLogin": "clee" },
    { "email": "ldelplaya@ucsb.edu", "firstName": "Lauren",  "legalFirstName": "LAUREN",       "githubLogin": "ldelplaya" }
  ]
}
```

Field by field:

| Field | Meaning |
|---|---|
| `email` | The student's email as stored on the roster |
| `firstName` | The shortest name that still identifies the student among all students of the course: the first name alone if nobody else shares it, otherwise first name plus last initial, otherwise first and last name (with a `*` if even that is shared). These are the same names as on the team CSVs, and the name a teammate is most likely to type |
| `legalFirstName` | The roster's first name field, unchanged (may include a middle name) |
| `lastName` | The roster's last name field, unchanged |
| `githubLogin` | The student's GitHub login, or `null` if they have not linked GitHub yet |
| `team` | The student's team, or `null` if they are not on one. A student on several teams gets the first one |
| `teams` | All of the student's teams, possibly empty |
| `teamMembers` | Everyone on `team`, the student included, sorted by name; empty if `team` is `null` |

#### Using it from a Gradescope autograder

Gradescope puts the submitter's email in `/autograder/submission_metadata.json`. A complete lookup,
with [`jq`](https://jqlang.github.io/jq/), is:

```bash
FRONTIERS_URL=https://frontiers.dokku-00.cs.ucsb.edu
COURSE_ID=2
API_KEY=$(< /autograder/source/frontiers_api_key)   # a file in the autograder zip, never in git

STUDENT_EMAIL=$(jq -r '.users[0].email' < /autograder/submission_metadata.json)

if ! curl -s -f "$FRONTIERS_URL/api/courses/studentInfo?courseId=$COURSE_ID&email=$STUDENT_EMAIL" \
        -H "X-API-KEY: $API_KEY" > STUDENT_INFO.json; then
  echo "Could not find $STUDENT_EMAIL on the Frontiers roster for this course."
  exit 1
fi

GITHUB_LOGIN=$(jq -r '.githubLogin // empty' < STUDENT_INFO.json)
TEAM=$(jq -r '.team // empty' < STUDENT_INFO.json)
TEAMMATE_NAMES=$(jq -r '.teamMembers[].firstName' < STUDENT_INFO.json)
```

Fetch once per submission and keep the file; the key's usage count goes up on every call.

**Testing the autograder as staff.** Course staff are not roster students, so a staff member's email
is not found by this endpoint. To try an autograder from a student's point of view, add the staff
member to the roster by hand (Students tab, add student) and put them on a team.

## Revoking a key

Revoking is permanent. There are two ways:

* **From the API Keys tab, as the instructor:** click **Revoke** on the key's row and confirm. Only
  active keys have a Revoke button. From Swagger, take the `id` from the list above and call
  ```
  DELETE /api/courses/key?courseId=2&id=7
  ```
  `200` with `{"message": "API key with id 7 revoked"}` means it is revoked; `404` means there is no
  key with that id in that course.

* **With the key itself, no login needed.** Anyone who holds a key can revoke it, so a script that
  discovers its key has leaked can disable it at once:
  ```bash
  curl -s -X DELETE 'https://frontiers.dokku-00.cs.ucsb.edu/api/courses/key/revoke?apiKey=u1Zc6N0T0dG9xWcY4h2JbQ'
  ```
  `204` means it is revoked; `404` means it was not a key of this Frontiers instance.

## Implementation notes (for developers)

* Keys are 128 random bits from `SecureRandom`, URL-safe Base64 encoded. Only the SHA-256 hash and
  the last six characters are stored (`CourseApiKey`).
* `ApiKeyFilter` turns a valid `X-API-KEY` header into an `ApiKeyToken` with the single role
  `ROLE_API_KEY` and the key's course id. `CourseSecurity.hasManagePermissions` and
  `hasInstructorPermissions` accept such a token only when its course id equals the `courseId` of
  the request.
* `ApiKeyInterceptor` refuses any API-key request whose handler method lacks `@AllowApiKeyAccess`.
* Because API-key requests carry no session, CSRF protection is skipped for them
  (`SecurityConfig`); it remains on for every other request.
