import { toast } from "react-toastify";

/**
 * Query key for the (global) list of dokku account translations. The list
 * itself is not per-course, but it is always fetched from a course's Dokku
 * tab, which passes the courseId for authorization.
 */
export function dokkuTranslationsQueryKey(courseId) {
  return `/api/dokku/translations?courseId=${courseId}`;
}

/**
 * URL of the dokku_users_list.csv download for a course.
 */
export function dokkuUsersListUrl(courseId) {
  return `/api/dokku/dokku_users_list?courseId=${courseId}`;
}

/**
 * Shared onError handler for dokku account translation create/update
 * mutations. A 409 from the backend means that email already has a
 * translation.
 */
export function onDokkuTranslationMutationError(error) {
  if (error.response.status === 409) {
    toast("A dokku account translation for that email already exists.");
  } else {
    toast(`${error}`);
  }
}
