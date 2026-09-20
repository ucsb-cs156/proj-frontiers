import { toast } from "react-toastify";

/**
 * Shared onError handler for section create/update mutations. A 409 from the
 * backend means the (course, section) pair already exists.
 */
export function onSectionMutationError(error) {
  if (error.response.status === 409) {
    toast("A section with that section value already exists for this course.");
  } else {
    toast(`${error}`);
  }
}
