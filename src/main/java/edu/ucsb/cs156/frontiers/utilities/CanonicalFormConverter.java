package edu.ucsb.cs156.frontiers.utilities;

public class CanonicalFormConverter {

    /**
     * Converts an email address to a valid canonical form.
     * 
     * Some universities may have email systems where there are 
     * multiple valid representations of the same email address.
     * 
     * This method ensures that the email is in a consistent
     * canonical form.   This is the place to isolate these
     * conversions, so that if the rules change, we only have
     * to change them in one place.
     * 
     * @param email
     * @return email in canonical form
     */
    public static String convertToValidEmail(String email) {
       String canonicalEmail = email.replace("@umail.ucsb.edu", "@ucsb.edu")
                                    .toLowerCase();
       return canonicalEmail;
    }

    /** Check whether two emails are equivalent in their canonical form
     * @param email1
     * @param email2
     * @return true if the canonical forms of the two emails are equal, false otherwise
     */
    public static boolean areEquivalentEmails(String email1, String email2) {
        return convertToValidEmail(email1).equals(convertToValidEmail(email2));
    }
}
