# Dokku Environment Variables Guide (CS156)

This guide documents the procedures and best practices for setting up and managing environment variables on Dokku instances for CS156 applications.

---

## 🛠️ Setting Environment Variables (`dokku config:set`)

To define environment variables on your Dokku application, use `dokku config:set`. We highly recommend using the `--no-restart` flag to prevent your app from repeatedly restarting while setting multiple variables.

```bash
dokku config:set --no-restart appname VARIABLE_NAME=value
```

---

## 🔑 Crucial Environment Variables

### 1. `PRODUCTION=true`
This is a required variable for almost all CS156 apps.
```bash
dokku config:set --no-restart appname PRODUCTION=true
```
> **What this does:** It signals the build process (via Maven `pom.xml`) to compile and optimize the React frontend, and bundle it directly into the Spring Boot backend JAR file. When deployed, the app will serve both the backend APIs and frontend pages from a single integrated server on port `5000`.

### 2. `ADMIN_EMAILS`
Defines which users have administrative privileges (e.g., viewing user logs, executing system jobs).
```bash
dokku config:set --no-restart appname ADMIN_EMAILS=email1,email2,email3
```

> **Syntax Rules:**
> - Must be a **comma-separated list** with **no spaces**.
> - ❌ **WRONG:** `ADMIN_EMAILS=phtcon@ucsb.edu, cgaucho@ucsb.edu`
> - ✅ **CORRECT:** `ADMIN_EMAILS=phtcon@ucsb.edu,cgaucho@ucsb.edu`
>
> **Recommended List of Emails:**
> - Your instructor's email (`phtcon@ucsb.edu`)
> - Your team's mentor's email
> - Every member of your project team

---

## 📋 Listing Defined Environment Variables

To check what variables are currently set on your Dokku application, run:
```bash
dokku config:show appname
```

### Example Output:
```text
=====> pconrad-jpa03 env vars
DATABASE_URL:          postgres://postgres:a6cbThisPasswordIsFakeb@dokku-postgres-pconrad-jpa03-db:5432/pconrad_jpa03_db
DOKKU_APP_RESTORE:     1
DOKKU_APP_TYPE:        herokuish
DOKKU_PROXY_PORT:      80
DOKKU_PROXY_PORT_MAP:  http:80:5000
GIT_REV:               631f1ac48d19d9c39d28bb071fed1ec8fdee0aaf
GOOGLE_CLIENT_ID:      26622685272-ofq4729s9nt8loednuuv5c0opja1vaeb.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET:  GOCSPX-fakeCredentials99_fakefake-_fake
POSTGRES:              true
PRODUCTION:            true
```
