# Deploying a Web App to Dokku (CS156 Guide)

This guide provides step-by-step instructions for deploying applications to the UCSB CS156 Dokku cluster, particularly for apps requiring a database, frontend integration, and OAuth authentication.

---

## 📋 Overview of Steps
1. **Login** to your Dokku machine
2. **Create** the app on Dokku
3. **Define Environment Variables** (including `PRODUCTION=true`)
4. **Define Settings** (like keeping the `.git` directory)
5. **Create and Link Postgres Database**
6. **Sync with GitHub Repo**
7. **Build App with HTTP**
8. **Enable HTTPS** via Let's Encrypt
9. **Test OAuth**

---

## 🛠️ Step-by-Step Implementation

### Step 1: Log in to your Dokku machine
SSH into your allocated Dokku server instance (e.g., `dokku-00.cs.ucsb.edu` or via CSIL).

### Step 2: Create the App
Create the app using the `apps:create` command:
```bash
dokku apps:create appname
```
*Note: Replace `appname` with your specific app name (e.g., `frontiers-dev-parm2006` or `frontiers-parm2006`).*

### Step 3: Define Environment Variables
You must set `PRODUCTION=true` to enable serving the frontend and backend from the same integrated server:
```bash
dokku config:set --no-restart appname PRODUCTION=true
```

Set each additional environment variable from your `.env` file (e.g., `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `WEBHOOK_SECRET`, `ADMIN_EMAILS`):
```bash
dokku config:set appname --no-restart VARIABLE_NAME=value
```

#### ⚡ Shortcut: Bulk Upload Environment Variables
Instead of setting variables one by one, you can bulk import them:
1. On your development machine, view your `.env` contents.
2. In your active Dokku session terminal, run:
   ```bash
   cat > appname.env
   ```
3. Paste the contents of your `.env` file directly into the terminal, hit **Enter**, and then press **`Ctrl + D`**.
4. Set all variables at once from the created file:
   ```bash
   dokku config:set --no-restart appname `cat appname.env`
   ```

### Step 4: Keep Git Directory
Our codebase depends on the `.git` directory to display commit/branch metadata in the header. Configure Dokku to retain this directory:
```bash
dokku git:set appname keep-git-dir true
```

### Step 5: Create and Link Postgres Database
Initialize and link a Postgres database to your app:
```bash
dokku postgres:create appname-db
dokku postgres:link appname-db appname
```

### Step 6: Sync with GitHub Repo
Point your Dokku application to synchronize with your public or private GitHub repository:
```bash
dokku git:sync appname https://github.com/owner/repo.git main
```

### Step 7: Build App with HTTP (First Run)
Rebuild the container image for the first time on HTTP:
```bash
dokku ps:rebuild appname
```
*Note: OAuth logins will not function yet, as HTTPS is required for secure authentication callbacks.*

### Step 8: Enable HTTPS
Secure your site and enable OAuth redirects by setting up Let's Encrypt SSL certificates:
```bash
dokku letsencrypt:set appname email yourEmail@ucsb.edu
dokku letsencrypt:enable appname
```

### Step 9: Test OAuth
Open your browser and navigate to:
👉 `https://appname.dokku-xx.cs.ucsb.edu` (where `xx` is your dokku server number).

Verify you can successfully log in using Google/GitHub OAuth!
