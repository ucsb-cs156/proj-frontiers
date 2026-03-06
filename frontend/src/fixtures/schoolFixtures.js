const schoolFixtures = {
  chicoState: {
    alternateNames: ["Chico State University", "Chico State", "CSUCS"],
    canvasImplementation: "https://canvas.csuchico.edu/",
    displayName: "Chico State",
    key: "CHICO_STATE",
  },
  oregonState: {
    alternateNames: ["Oregon State University", "Oregon State", "OSU"],
    canvasImplementation: "https://canvas.oregonstate.edu/",
    displayName: "Oregon State University",
    key: "OREGON_STATE",
  },
  ucsb: {
    alternateNames: [
      "UC Santa Barbara",
      "University of California, Santa Barbara",
      "SB",
    ],
    canvasImplementation: "https://ucsb.instructure.com/",
    displayName: "UCSB",
    key: "UCSB",
  },
  wsu: {
    alternateNames: ["Washington State University", "Washington State", "WSU"],
    canvasImplementation: "https://canvas.wsu.edu/",
    displayName: "Washington State University",
    key: "WSU",
  },
};

const schoolList = [
  schoolFixtures.chicoState,
  schoolFixtures.oregonState,
  schoolFixtures.ucsb,
  schoolFixtures.wsu,
];

export { schoolFixtures, schoolList };
