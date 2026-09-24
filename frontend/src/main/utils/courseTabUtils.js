// Which tab of the course page (InstructorCourseShowPage) is showing is kept
// in the URL, as ?tab=<name>, so that a tab can be linked to, and survives a
// refresh of the page. The tab last used for each course is also remembered
// in the browser's local storage, for when the page is opened without ?tab=.

// Every tab the page can have; some are only shown for some courses or users.
export const COURSE_TABS = [
  "students",
  "staff",
  "teams",
  "sections",
  "assignments",
  "jobs",
  "downloads",
  "dokku",
  "slack",
  "settings",
];

export const DEFAULT_COURSE_TAB = "assignments";

export function courseTabStorageKey(courseId) {
  return `frontiers.courseTab.${courseId}`;
}

// Local storage can be unavailable (for example, blocked by browser settings);
// remembering the tab is a convenience, so that is never treated as an error.
export function getStoredCourseTab(courseId) {
  try {
    return window.localStorage.getItem(courseTabStorageKey(courseId));
  } catch {
    return null;
  }
}

export function storeCourseTab(courseId, tab) {
  try {
    window.localStorage.setItem(courseTabStorageKey(courseId), tab);
  } catch {
    // ignored; see above
  }
}

// requestedTab: the value of ?tab=, or null if the URL has none
// storedTab: the tab remembered for this course, or null
// visibleTabs: the tabs that the page is showing at the moment
//
// A tab that is asked for but is not (yet) visible, for example the Slack tab
// while the course options are still loading, gives the default tab for now;
// since this is worked out on every render, the requested tab takes over as
// soon as it appears.
export function chooseCourseTab({ requestedTab, storedTab, visibleTabs }) {
  if (requestedTab !== null) {
    return visibleTabs.includes(requestedTab)
      ? requestedTab
      : DEFAULT_COURSE_TAB;
  }
  return visibleTabs.includes(storedTab) ? storedTab : DEFAULT_COURSE_TAB;
}
