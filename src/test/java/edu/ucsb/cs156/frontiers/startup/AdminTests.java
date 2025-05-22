package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminTests {

    @Test
    void noArgsConstructor_shouldYieldNullFields_andSettersShouldWork() {
        Admin a = new Admin();
        assertNull(a.getId());
        assertNull(a.getEmail());

        // exercise setters
        a.setId(42L);
        a.setEmail("foo@bar.com");
        assertEquals(42L, a.getId());
        assertEquals("foo@bar.com", a.getEmail());
    }

    @Test
    void allArgsConstructor_shouldSetBothFields() {
        Admin a = new Admin(7L, "alice@example.com");
        assertEquals(7L, a.getId());
        assertEquals("alice@example.com", a.getEmail());
    }

    @Test
    void customEmailConstructor_shouldSetEmail_only() {
        Admin a = new Admin("bob@example.com");
        assertNull(a.getId(), "id must remain null");
        assertEquals("bob@example.com", a.getEmail());
    }

    @Test
    void equalsAndHashCode_shouldBeBasedOnAllFields() {
        Admin a1 = new Admin(1L, "x@x.com");
        Admin a2 = new Admin(1L, "x@x.com");
        Admin a3 = new Admin(2L, "x@x.com");

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, a3);
    }

    @Test
    void toString_shouldContainFieldValues() {
        Admin a = new Admin(99L, "me@you.org");
        String s = a.toString();
        assertTrue(s.contains("99"));
        assertTrue(s.contains("me@you.org"));
    }
}
