package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.entities.Admin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminTests {

    @Test
    void noArgsConstructor_andSetterGetter_shouldWork() {
        Admin a = new Admin();
        // email starts out null
        assertNull(a.getEmail());

        // exercise setter/getter
        a.setEmail("foo@bar.com");
        assertEquals("foo@bar.com", a.getEmail());
    }

    @Test
    void allArgsConstructor_shouldSetEmail() {
        Admin a = new Admin("alice@example.com");
        assertEquals("alice@example.com", a.getEmail());
    }

    @Test
    void equalsAndHashCode_shouldBeBasedOnEmail() {
        Admin a1 = new Admin("x@x.com");
        Admin a2 = new Admin("x@x.com");
        Admin a3 = new Admin("y@y.com");

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, a3);
    }

    @Test
    void toString_shouldContainEmail() {
        Admin a = new Admin("me@you.org");
        String s = a.toString();
        assertTrue(s.contains("me@you.org"));
    }
}
