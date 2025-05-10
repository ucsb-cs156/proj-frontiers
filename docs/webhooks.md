## Webhooks
Frontiers makes use of GitHub's Webhooks functionality to detect when users have accepted the invitation to a Course's linked Github Organization.

These webhooks should be of this format, with unused properties omitted for brevity:

```json
{
  "action": "member_added",
  "installation_id": "<installation id of course>",
  "membership": {
    "organization_url": "https://github.com/<organization name>",
    "role": "<member or owner>",
    "user": {
      "login": "<login of user who joined the organization>",
      "id": <unique identifying number of user who joined as a number>
    }
  }
}
```

These are used to match the user and course in question, and mark their status in the organization.

However, GitHub cannot send the local testing environment webhooks. As a result, when creating the app, only the [directions for Dokku](github-app-setup-dokku.md) include setting up the webhook functionality.

Additionally, GitHub doesn't allow manually sending simulated events to test your webhook. As a result, for an actual webhook to be sent, a user will have to be invited and accept the invitation.

However, they can be simulated. They can be tested with cURL:
```bash
curl "http://localhost:8080/api/webhooks/github" \
-X POST \
-H "Content-Type: application/json" \
-d '{
  "action": "member_added",
  "installation": {
    "id": "<installation id of course>",
  }
  "membership": {
    "organization_url": "https://github.com/<organization name>",
    "role": "<member or owner>",
    "user": {
      "login": "<login of user who joined the organization>",
      "id": <unique identifying number of user who joined as a number>
    }
  }
}'
```

This will also function for Dokku, if the url is swapped for the appropriate Dokku app.
