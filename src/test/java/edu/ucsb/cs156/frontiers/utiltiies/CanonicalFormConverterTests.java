package edu.ucsb.cs156.frontiers.utiltiies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;

public class CanonicalFormConverterTests {
    @Test
    public void testConvertToValidEmail() {
        String email = "foo@umail.ucsb.edu";
        String expected = "foo@ucsb.edu";
        String actual = CanonicalFormConverter.convertToValidEmail(email);
        assertEquals(expected, actual);
    }

    /**
     * For test coverage purposes, we want to ensure that the
     * constructor of CanonicalFormConverter is called, even though
     * it does not have any logic in it.
     */
    @Test
    public void test_coverage_for_constructor() {
        CanonicalFormConverter converter = new CanonicalFormConverter();
        assertInstanceOf(CanonicalFormConverter.class, converter);
    }

    /**
     * Test the areEquivalentEmails method
     */
    @Test
    public void testAreEquivalentEmails() {
        assertTrue(CanonicalFormConverter.areEquivalentEmails("foo@umail.ucsb.edu", "foo@ucsb.edu"));
        assertFalse(CanonicalFormConverter.areEquivalentEmails("bar@ucsb.edu", "foo@ucsb.edu"));
    }

}
