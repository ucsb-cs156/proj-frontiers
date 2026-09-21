const slackFixtures = {
  connectedInfo: {
    courseId: "7",
    slackBotToken: "xoxb******************ghij",
    slackTeamId: "T12345678",
    slackTeamName: "ucsb-cs156-f26",
    slackTeamUrl: "https://ucsb-cs156-f26.slack.com/",
  },
  notConnectedInfo: {
    courseId: "7",
    slackBotToken: "",
    slackTeamId: "",
    slackTeamName: "",
    slackTeamUrl: "",
  },
  fourUsers: [
    {
      slackUserId: "U01",
      name: "pconrad",
      realName: "Phill Conrad",
      displayName: "phtcon",
      email: "phtcon@ucsb.edu",
      courseRole: "INSTRUCTOR",
    },
    {
      slackUserId: "U02",
      name: "cgaucho",
      realName: "Chris Gaucho",
      displayName: "chris",
      email: "cgaucho@ucsb.edu",
      courseRole: "STUDENT",
    },
    {
      slackUserId: "U03",
      name: "ldelplaya",
      realName: "Lauren Del Playa",
      displayName: "lauren",
      email: "ldelplaya@ucsb.edu",
      courseRole: "STAFF",
    },
    {
      slackUserId: "U04",
      name: "visitor",
      realName: "Some Visitor",
      displayName: "",
      email: "visitor@example.org",
      courseRole: "NONE",
    },
  ],
  threeMissingMembers: [
    {
      courseRole: "STAFF",
      firstName: "Sam",
      lastName: "Sabado",
      email: "ssabado@ucsb.edu",
      slackStatus: "NOT_IN_SLACK",
    },
    {
      courseRole: "STUDENT",
      firstName: "Taylor",
      lastName: "Trigo",
      email: "ttrigo@ucsb.edu",
      slackStatus: "INVITED",
    },
    {
      courseRole: "STUDENT",
      firstName: "Pat",
      lastName: "Pardall",
      email: "ppardall@ucsb.edu",
      slackStatus: "DEACTIVATED",
    },
  ],
};

export default slackFixtures;
