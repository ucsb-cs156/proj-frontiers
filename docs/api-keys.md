## Frontiers Course-Scoped API Keys
Frontiers provides the ability to create API keys. They can only be used to complete actions for a specific course.
Attempting to use one otherwise will result in an error. They can also be revoked at any time; You do not need
to sign in to do so. If a valid API key is provided to the revoke endpoint, it will be revoked. Additionally, they
can only be used on specific endpoints. For now, there's only one eligible endpoint:

| Endpoint            |
|---------------------|
| /api/courses/emails |


## Usage
API Keys can be used by providing the key in the `X-API-Key` header.

Here's a simple example that uses the `curl` command to request the `/api/courses/emails` endpoint for course 2:

```bash
COURSE_EMAILS="$(
curl -s -X 'GET' \
  'https://proj-frontiers-division7.dokku-00.cs.ucsb.edu/api/courses/emails?courseId=2&type=STUDENTS&format=ONE_PER_LINE' \
  -H 'X-API-KEY: fake-api-key'
)"
for email in $COURSE_EMAILS; do
  echo "student has email: $email"
done
```

For a course with students `ldelplaya@ucsb.edu` and `cgaucho@ucsb.edu`, this would return:
```
student has email: ldelplaya@ucsb.edu
student has email: cgaucho@ucsb.edu
```



