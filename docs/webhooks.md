## Webhooks

Frontiers makes use of GitHub's Webhooks functionality to detect when users have accepted the invitation to a Course's linked Github Organization.

## Security

As of version 1.0.0, webhooks are secured using HMAC-SHA256 signatures. All webhook requests must include a valid `X-Hub-Signature-256` header that matches the configured webhook secret. Requests without valid signatures are rejected with a 401 Unauthorized response.

The application requires a `WEBHOOK_SECRET` environment variable that is at least 10 characters long. If this requirement is not met, the application will fail to start.

## Webhook Format

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

However, they can be simulated. They can be tested with cURL, but you must include a valid signature:

```bash
# For testing, you need to generate a valid HMAC-SHA256 signature
# This example assumes WEBHOOK_SECRET="localhost_dev_secret_123"

PAYLOAD='{
  "action": "member_added",
  "installation": {
    "id": "<installation id of course>"
  },
  "membership": {
    "organization_url": "https://github.com/<organization name>",
    "role": "<member or owner>",
    "user": {
      "login": "<login of user who joined the organization>",
      "id": <unique identifying number of user who joined as a number>
    }
  }
}'

SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "localhost_dev_secret_123" | sed 's/^.* //')

curl "http://localhost:8080/api/webhooks/github" \
-X POST \
-H "Content-Type: application/json" \
-H "X-Hub-Signature-256: sha256=$SIGNATURE" \
-d "$PAYLOAD"
```

For production environments, replace `localhost_dev_secret_123` with your actual webhook secret.

This will also function for Dokku, if the url is swapped for the appropriate Dokku app.
