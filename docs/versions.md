# Updating Versions of Java and/or node

## Updating the Java version

When updating the version of Java used, the following places need to be adjusted:

* `Versions` section of the README.md
* `pom.xml` file (`java.version` property; also check that the `spring-boot-starter-parent`
  version and build plugins such as Jacoco, Pitest and google-java-format support the new Java version)
* `.java-version` file (used by Github Actions scripts)
* `system.properties` file (used by the Dokku/Heroku Java buildpack)
* `Dockerfile` used for deploying on Dokku
* `.github/copilot-instructions.md` (JAVA_HOME path used by Copilot coding agent)

## Updating the node version

Places that name the node version:

* `Versions` section of the README.md
* `engines` section in `frontend/package.json`. This also controls CI: the shared workflows in
  `ucsb-cs156/workflows` and this repo's Chromatic and gh-pages workflows call `actions/setup-node`
  with `node-version-file: frontend/package.json`, so no workflow edit is needed.
* `pom.xml`: the `app.frontend.nodeVersion` property, used by `frontend-maven-plugin` in the
  `integration` and `production` profiles
* `Dockerfile` used for deploying on Dokku (`ENV NODE_VERSION=...`)
* `.github/copilot-instructions.md` (the `nvm use` commands for the Copilot coding agent)

Then check `grep -rIn "<old version>" --exclude-dir=node_modules --exclude-dir=target .` for anything else.

After changing the version, if a local `mvn` build fails in `npm ci` with
`Class extends value undefined is not a constructor or null`, delete `target/node` and
`target/node_modules` (stale npm from the previous node version left in the plugin's install dir).

### Updating frontend dependencies at the same time

Notes from the move to node 24.21.0 (issue #800; the same update was first done in
`ucsb-cs156/proj-courses` issue #355 / PR #356, whose notes were the starting checklist):

* `npm audit`, `npm outdated` and `npm ci 2>&1 | grep -i "deprecated\|warn"` show what needs attention.
  Remove dependencies that are not imported anywhere (`react18-json-view`, `@vitejs/plugin-react`
  (the swc plugin is the one in use), `@storybook/addon-onboarding`, `prop-types`, `typescript-eslint`
  were unused here). ESLint 9 only reads `eslint.config.mjs`, so the CRA-era `eslintConfig` block in
  `package.json` was dead and was removed too.
* `frontend/.npmrc` sets `min-release-age=7`, so `npm install` refuses any version published in the
  last 7 days with a confusing `ETARGET No matching version found for <pkg>@^x.y.z with a date before
  ...`. `npm view <pkg> version` ignores this setting; use the `Latest` column of `npm outdated`
  (which respects it) when choosing versions, or pick the previous patch release.
* If `npm install` fails with a confusing `ERESOLVE` after editing versions, regenerate:
  `rm -rf node_modules package-lock.json && npm install`.
* npm 11 blocks dependency install scripts by default and warns about the ones it skipped. After
  checking they are expected (`esbuild`, `@swc/core`, `msw`, `fsevents` here), run
  `npm install-scripts approve <pkg>` for each and commit the resulting `allowScripts` block in
  `package.json`. The entries pin exact versions, so they need re-approving after those packages are
  bumped. Do not approve `storybook-addon-remix-react-router` 5.x: its preinstall is `npx only-allow
  pnpm` (removed in 7.x).
* The msw upgrade regenerates `frontend/public/mockServiceWorker.js` (postinstall); commit it.
* Verify with all of: `npm run lint`, `npm run check-format`, `npm test` (repeat a few times to catch
  flakiness), `npm run build`, `npm run build-storybook`, and Stryker on the changed files using the
  same command as the CI job (`33-frontend-pr-mutation-testing` in `ucsb-cs156/workflows`), and a
  `mvn -Pproduction -DskipTests package` build. Unit tests alone do not catch Storybook or Stryker
  breakage.
* Breaking changes hit in this repo:
  * react-router 8 is ESM-only, so `vi.spyOn(reactRouter, "useLocation")` on the module namespace
    throws `Cannot spy on export ... Module namespace is not configurable in ESM`. Use
    `vi.mock("react-router", async (importOriginal) => ({ ...await importOriginal(), useLocation: ... }))`
    instead (see `HelpCsvPage.test.jsx`). All imports were already from `react-router` (v8 drops
    `react-router-dom`); no other code changes were needed. v8 also requires React 19.2.7+.
  * jsdom 30 resolves `rem` to `px` in `getComputedStyle` (`18rem` becomes `288px`), so
    `toHaveStyle("width: 18rem")` fails. Assert on the inline style instead
    (`expect(el.style.width).toBe("18rem")`); `%`, `px`, colors and `transform` are unaffected.
  * Two tests asserted synchronously that the course modal was gone right after the create
    mutation's `onSuccess` toast; they now `await waitFor(...)` for the unmount.
  * Storybook 10 + msw-storybook-addon 3: use `mswLoader` from `msw-storybook-addon/csf3`, call
    `mswLoader()`, drop `initialize()`, and list `msw-storybook-addon` in `addons`.
    `.storybook/main.js` was renamed to `main.mjs`. `storybook-addon-remix-react-router` 7 is the
    version for Storybook 10 / react-router 8 and is now also listed in `addons`.
  * vite 8 (Rolldown): the config was already `vite.config.mjs`, but the up-to-date check in
    `pom.xml` still pointed at `vite.config.js` (fixed); `rollupOptions` is now `rolldownOptions` and
    `__dirname` is `import.meta.dirname`.
* Held back on purpose:
  * `vitest`/`@vitest/coverage-v8` stay on 4.x: with vitest 5, Stryker's vitest runner maps no tests
    to mutants, so every mutant survives (found in proj-courses).
  * `eslint`/`@eslint/js` stay on 9.x: `eslint-plugin-react` 7.37.5 (still the latest) crashes on
    ESLint 10 (`context.getFilename is not a function`). This is the one remaining `npm ci`
    deprecation warning.
  * `@tanstack/react-table` stays on 8 (9 is a rewrite).
