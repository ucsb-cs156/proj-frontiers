/**
 * Format a timestamp into a human-readable date and time
 * @param {string} timestamp - ISO format timestamp string
 * @returns {string} - Formatted date and time string
 */
export function formatTime(timestamp) {
  if (!timestamp) {
    return "";
  }

  const date = new Date(timestamp);
  return date.toLocaleString();
}
