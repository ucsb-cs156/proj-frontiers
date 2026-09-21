package edu.ucsb.cs156.frontiers.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SlackUserTests {

  @Test
  void email_and_displayName_come_from_profile() {
    SlackUser user =
        SlackUser.builder()
            .profile(SlackUser.Profile.builder().email("a@ucsb.edu").displayName("a").build())
            .build();
    assertEquals("a@ucsb.edu", user.email());
    assertEquals("a", user.displayName());
  }

  @Test
  void email_and_displayName_are_null_without_profile() {
    SlackUser user = SlackUser.builder().build();
    assertNull(user.email());
    assertNull(user.displayName());
  }

  @Test
  void isPerson_excludes_bots_and_slackbot() {
    assertTrue(SlackUser.builder().id("U01").build().isPerson());
    assertTrue(SlackUser.builder().build().isPerson());
    assertFalse(SlackUser.builder().id("U01").bot(true).build().isPerson());
    assertFalse(SlackUser.builder().id("USLACKBOT").build().isPerson());
  }

  @Test
  void isActivePerson_excludes_deactivated_invited_and_non_people() {
    assertTrue(SlackUser.builder().id("U01").build().isActivePerson());
    assertFalse(SlackUser.builder().id("U01").deleted(true).build().isActivePerson());
    assertFalse(SlackUser.builder().id("U01").invitedUser(true).build().isActivePerson());
    assertFalse(SlackUser.builder().id("U01").bot(true).build().isActivePerson());
  }

  @Test
  void nextCursor_is_empty_when_absent() {
    assertEquals("", new SlackUsersListResponse().nextCursor());
    assertEquals(
        "",
        SlackUsersListResponse.builder()
            .responseMetadata(new SlackUsersListResponse.ResponseMetadata())
            .build()
            .nextCursor());
    assertEquals(
        "abc",
        SlackUsersListResponse.builder()
            .responseMetadata(new SlackUsersListResponse.ResponseMetadata("abc"))
            .build()
            .nextCursor());
  }
}
