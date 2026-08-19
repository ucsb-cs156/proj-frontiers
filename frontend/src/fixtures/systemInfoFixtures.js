const systemInfoFixtures = {
  showingBoth: {
    springH2ConsoleEnabled: true,
    showSwaggerUILink: true,
    oauthLogin: "/oauth2/authorization/google",
  },
  showingNeither: {
    springH2ConsoleEnabled: false,
    showSwaggerUILink: false,
    oauthLogin: "/oauth2/authorization/google",
  },
  oauthLoginUndefined: {
    springH2ConsoleEnabled: false,
    showSwaggerUILink: false,
  },
  withActiveDirectory: {
    springH2ConsoleEnabled: false,
    showSwaggerUILink: false,
    activeDirectoryUrl: "/oauth2/authorization/azure-dev",
  },
  withActiveDirectoryAndGoogle: {
    springH2ConsoleEnabled: false,
    showSwaggerUILink: false,
    oauthLogin: "/oauth2/authorization/google",
    activeDirectoryUrl: "/oauth2/authorization/azure-dev",
  },
  showingAll: {
    springH2ConsoleEnabled: true,
    showSwaggerUILink: true,
    oauthLogin: "/oauth2/authorization/google",
    sourceRepo: "https://github.com/ucsb-cs156/proj-frontiers",
    commitMessage: "Add Developer Info page",
    commitId: "1234567890abcdef1234567890abcdef12345678",
    githubUrl:
      "https://github.com/ucsb-cs156/proj-frontiers/commit/1234567890abcdef1234567890abcdef12345678",
  },
};

export { systemInfoFixtures };
