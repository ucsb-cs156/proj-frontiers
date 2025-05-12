# Intro to Frontiers & GitHub REST API Integration

## About Frontiers
Frontiers is a platform designed to replace the old GitHub-linker tool used by UCSB's Computer Science Department. Its primary purpose is to use the **GitHub REST API** to automate the creation of student repositories and manage course-specific GitHub organizations.

---

## Summary of GitHub REST API in Frontiers

### **Types of Interactions with GitHub REST API**
1. **Acting on behalf of a user**:
    - Requires **GitHub tokens** (e.g., Personal Access Tokens or OAuth tokens).
    - Typically used when endpoints require user-level permissions.
    - **Note:** Frontiers does not permit installation on user accounts; it only integrates with organizations.

2. **Acting as an application**:
    - Requires a **JWT** for authentication or an **installation token** for specific actions.
    - Changes made are attributed to the application (not a user).

---

### **Authentication and Authorization in GitHub API**
GitHub REST API endpoints are secured using one of two mechanisms:
1. **JWT (JSON Web Token):**
    - Used to authenticate the application itself.
    - Enables actions like:
        - Viewing where the app is installed.
        - Managing the application settings.
        - Obtaining installation tokens.
    - Signed using the app’s private key provided by GitHub.

2. **GitHub Installation Tokens:**
    - Installation tokens act on behalf of a specific application installation.
    - Permissions are scoped to the installation, often tied to an organization (e.g., repositories, members, etc.).
    - Example: The installation "ucsb-cs156" would only have access to the `ucsb-cs156` organization's resources and not others like `ucsb-cs156-s25`.

   **Header Format for Requests**:
   ```http
   Authorization: Bearer <token>
   ```

---

### **How Authentication is Abstracted in Frontiers**
- **JWT Generation**: Managed by `JwtService.getJwt()`, used to create signed tokens for application-level actions.
- **Installation Tokens**: Managed by `JwtService.getInstallationToken(String installationId)`, enabling scoped access tied to a specific GitHub organization installation.

---

### **Course Integration with GitHub**
- Frontiers links each **GitHub installation** to a **Course** entity in the application.
- Key attributes:
    - `installationId`: Used to generate a GitHub installation token.
    - `organization`: Represents the GitHub organization tied to the course.
- Once linked, the app can access GitHub REST API resources related to that course’s organization (e.g., repositories, members).

---

### **Limitations and Restrictions**
- Frontiers prevents installation and interactions with user-level accounts—installations are restricted to organizations only.
- Endpoints requiring user credentials or admin-level access cannot be accessed.

---

### **Relevant Endpoints in GitHub REST API**
1. **Managing Installations (via JWT and installation token):**
    - Create repository: `POST /orgs/:org/repos`
    - List repositories: `GET /installation/repositories`
    - Manage repository permissions: `PUT /repos/:owner/:repo/collaborators/:username`

2. **Course-Scoped Integrations:**
    - Repositories and members for a course are scoped to the organization defined in the `Course` entity.

---

## Key Takeaways
Frontiers provides a streamlined integration with GitHub REST API for managing course-specific resources within GitHub organizations. By leveraging **JWTs** for authentication and **installation tokens** for scoped actions, the platform ensures tight permission control and compatibility with GitHub resources.

For more details on specific endpoints, refer to the [GitHub REST API Documentation](https://docs.github.com/en/rest).