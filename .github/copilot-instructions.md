# Frontiers - GitHub Copilot Instructions

Frontiers is a Spring Boot Java backend application with a React frontend that supports NSF Frontiers project for software engineering education. The application helps instructors manage GitHub organizations, student rosters, and code evaluation.

**ALWAYS reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

## Working Effectively

### Prerequisites and Setup
- Java 21 (Spring Boot requires exactly this version)
- Node.js >= 22.18.0 (frontend requirement)
- Maven 3.x for backend builds
- Required for full functionality: OAuth setup with Google and GitHub App setup

### Essential Build Commands
**NEVER CANCEL builds or long-running commands. Wait for completion.**

#### Backend (Java/Spring Boot)
- Set Java 21: `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 && export PATH=$JAVA_HOME/bin:$PATH`
- Compile: `mvn compile` -- takes ~3 seconds. NEVER CANCEL.
- Test: `mvn test` -- takes ~4 minutes. NEVER CANCEL. Set timeout to 10+ minutes.
- Full build: `mvn package` -- takes ~4 minutes. NEVER CANCEL. Set timeout to 10+ minutes.
- Integration tests: `INTEGRATION=true mvn test-compile failsafe:integration-test` -- takes ~70 seconds. NEVER CANCEL. Set timeout to 5+ minutes.

#### Frontend (React/Node.js)
- Set Node 22.18.0: `export NVM_DIR="$HOME/.nvm" && [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" && nvm use 22.18.0`
- Install dependencies: `cd frontend && npm ci` -- takes ~4 minutes. NEVER CANCEL. Set timeout to 10+ minutes.
- Build: `npm run build` -- takes ~20 seconds.
- Test: `npm test -- --watchAll=false` -- takes ~11 seconds.
- Format check: `npm run check-format` -- takes ~1 second.
- Format fix: `npm run format` -- takes ~1 second.

### Running the Application

#### Development Mode (Two Terminal Windows)
**Terminal 1 - Backend:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
mvn spring-boot:run
```
Backend starts on http://localhost:8080 in ~6 seconds.

**Terminal 2 - Frontend:**
```bash
export NVM_DIR="$HOME/.nvm" && [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" && nvm use 22.18.0
cd frontend
npm start
```
Frontend dev server starts on http://localhost:3000.

#### Integrated Mode (Single Command)
The Spring Boot backend at http://localhost:8080 serves the built frontend when properly configured.

#### Storybook (Component Development)
```bash
cd frontend
npm run storybook
```
Runs on http://localhost:6006

## Validation

### Manual Testing Requirements
**ALWAYS run through complete end-to-end scenarios after making changes:**
1. **Basic Application Load**: Navigate to http://localhost:8080 and verify the homepage loads with "Welcome to Frontiers!" header
2. **Navigation Test**: Click navigation links (if any) and verify they work
3. **Build Validation**: Verify both `mvn compile` and `npm run build` complete successfully
4. **Test Validation**: Run both `mvn test` and `npm test` and verify all tests pass

### Critical Validation Steps
- ALWAYS test the login flow if you modify authentication components
- ALWAYS verify the integrated application works on port 8080 after frontend changes
- ALWAYS run `npm run check-format` before committing frontend changes or the CI will fail
- ALWAYS run `mvn test` after backend changes to ensure no regressions

## Configuration Requirements

### OAuth Setup Required
The application requires OAuth setup to function properly. Without it, you'll see error 401: invalid_client.
- Copy `.env.SAMPLE` to `.env`
- Follow instructions in `docs/oauth.md` for Google OAuth setup
- Follow instructions in `docs/github-app-setup-localhost.md` for GitHub App setup

### Database
- Development: H2 in-memory database (automatic)
- Production: PostgreSQL (requires separate setup)
- H2 Console: http://localhost:8080/h2-console

## Common Issues and Solutions

### Build Issues
- **Java version mismatch**: Ensure Java 21 is active with `java --version`
- **Node version mismatch**: Ensure Node >= 22.18.0 with `node --version`
- **Frontend cache issues**: Delete `frontend/node_modules` and run `npm ci` again
- **Integration test failures**: Integration tests may fail due to Playwright browser dependencies in containerized environments

### Application Issues
- **Blank page on localhost:8080**: Wait a minute and refresh - the application takes time to start
- **Login errors**: Verify `.env` file exists and has proper OAuth credentials
- **Frontend not loading**: Ensure both backend and frontend are running in development mode

## Repository Structure

### Key Directories
- `/src/main/java/edu/ucsb/cs156/frontiers/` - Java backend source
- `/frontend/src/main/` - React frontend source
  - `/components/` - React components
  - `/pages/` - React pages/routes
  - `/utils/` - Utility functions
- `/docs/` - Documentation including OAuth setup
- `/.github/workflows/` - CI/CD workflows

### Important Files
- `pom.xml` - Maven configuration and dependencies
- `frontend/package.json` - Node.js dependencies and scripts
- `.env.SAMPLE` - Environment variable template
- `.java-version` - Java version specification (21)

## CI/CD Information

### GitHub Actions Timeouts
- Backend tests: 10 minutes (typical: 4 minutes)
- Frontend tests: 10 minutes (typical: ~11 seconds)
- Integration tests: Use headless mode with HEADLESS=false for debugging

### Workflow Files
- `12-backend-jacoco.yml` - Java test coverage
- `30-frontend-tests.yml` - Frontend Jest tests
- `35-frontend-format.yml` - Frontend code formatting check

## Development Tools

### Available Commands
- **Backend**: `mvn spring-boot:run`, `mvn test`, `mvn compile`
- **Frontend**: `npm start`, `npm test`, `npm run build`, `npm run storybook`
- **Format**: `npm run check-format`, `npm run format`
- **Coverage**: `npm run coverage` (frontend), `mvn jacoco:report` (backend)

### API Access
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 Console: http://localhost:8080/h2-console

## Deployment Information

### Local Development
- Frontend dev server: http://localhost:3000
- Backend API: http://localhost:8080
- Integrated app: http://localhost:8080
- Storybook: http://localhost:6006

### Production Environments
- Production: https://frontiers.dokku-00.cs.ucsb.edu
- QA: https://frontiers-qa.dokku-00.cs.ucsb.edu

## Performance Expectations

### Build Times (add 50% buffer for timeouts)
- Maven compile: ~3 seconds (timeout: 2 minutes)
- Maven test: ~4 minutes (timeout: 10 minutes)
- Maven integration tests: ~70 seconds (timeout: 5 minutes)
- npm ci: ~4 minutes (timeout: 10 minutes)
- npm build: ~20 seconds (timeout: 2 minutes)
- npm test: ~11 seconds (timeout: 2 minutes)

### Startup Times
- Spring Boot backend: ~6 seconds
- React dev server: ~15 seconds
- Storybook: ~15 seconds

**CRITICAL**: Never cancel builds or tests. They may appear to hang but are processing large dependency downloads or running comprehensive test suites.