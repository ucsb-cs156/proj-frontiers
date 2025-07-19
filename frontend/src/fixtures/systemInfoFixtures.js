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
};

export { systemInfoFixtures };
