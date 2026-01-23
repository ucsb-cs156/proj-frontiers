/**
 * Mapping of login providers to their associated schools
 */
export const loginProviderSchools = {
  google: ["Chico State University", "University of California, Santa Barbara"],
  microsoft: ["Oregon State University"],
};

export const schoolToProvider = Object.entries(loginProviderSchools).flatMap(
  ([provider, schools]) =>
    schools.map((school) => ({
      schoolName: school,
      provider: provider,
    })),
);
