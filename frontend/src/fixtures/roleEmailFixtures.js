const roleEmailFixtures = {
  oneItem: {
    email: "instructor1@example.com",
  },
  threeItems: [
    {
      email: "instructor1@example.com",
    },
    {
      email: "admin1@example.com",
    },
    {
      email: "instructor2@example.com",
    },
  ],
  threeItemsWithIsInAdminEmailField: [
    {
      email: "instructor1@example.com",
      isInAdminEmails: true,
    },
    {
      email: "admin1@example.com",
      isInAdminEmails: false,
    },
    {
      email: "instructor2@example.com",
      isInAdminEmails: false,
    },
  ],
};

export { roleEmailFixtures };
