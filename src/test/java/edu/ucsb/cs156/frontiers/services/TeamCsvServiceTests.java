package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.models.NameAndTeam;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TeamCsvServiceTests {

  private final TeamCsvService service = new TeamCsvService();

  private static RosterStudent student(String first, String last, String... teams) {
    List<TeamMember> members = new ArrayList<>();
    for (String team : teams) {
      members.add(TeamMember.builder().team(Team.builder().name(team).build()).build());
    }
    return RosterStudent.builder().firstName(first).lastName(last).teamMembers(members).build();
  }

  private static NameAndTeam nt(String name, String team) {
    return new NameAndTeam(name, team);
  }

  // titleCase

  @Test
  public void titleCase_handles_null_and_blank() {
    assertEquals("", service.titleCase(null));
    assertEquals("", service.titleCase(""));
    assertEquals("", service.titleCase("   "));
  }

  @Test
  public void titleCase_capitalizes_first_letter_of_each_word() {
    assertEquals("Chris", service.titleCase("CHRIS"));
    assertEquals("Chris", service.titleCase("chris"));
    assertEquals("Chris", service.titleCase("Chris"));
    assertEquals("Chris", service.titleCase("  cHRIS  "));
    assertEquals("De La Cruz", service.titleCase("DE LA CRUZ"));
    assertEquals("Mary-Jane O'Brien", service.titleCase("MARY-JANE O'BRIEN"));
    assertEquals("Mcdonald", service.titleCase("McDONALD"));
    assertEquals("X Æ A-12", service.titleCase("x æ a-12"));
  }

  // firstWord

  @Test
  public void firstWord_handles_null_and_blank() {
    assertEquals("", service.firstWord(null));
    assertEquals("", service.firstWord(""));
    assertEquals("", service.firstWord("   "));
  }

  @Test
  public void firstWord_returns_text_before_the_first_space() {
    assertEquals("Chris", service.firstWord("Chris"));
    assertEquals("Chris", service.firstWord("Chris Edward"));
    assertEquals("Chris", service.firstWord("  Chris   Edward Lee "));
    assertEquals("Mary-Jane", service.firstWord("Mary-Jane Ann"));
    assertEquals("CHRIS", service.firstWord("CHRIS\tEDWARD"));
  }

  // nameAndTeams

  @Test
  public void nameAndTeams_ignores_middle_names_in_the_first_name_field() {
    List<NameAndTeam> result =
        service.nameAndTeams(
            List.of(
                student("CHRIS EDWARD", "GAUCHO", "s26-01"),
                student("Chris", "Garcia", "s26-02"),
                student("Mary Ann", "Lee", "s26-03"),
                student("Pat Lee", "Smith", "s26-04"),
                student("Pat", "Smith", "s26-05")));
    assertEquals(
        List.of(
            nt("Chris Gaucho", "s26-01"),
            nt("Chris Garcia", "s26-02"),
            nt("Mary", "s26-03"),
            nt("Pat Smith*", "s26-04"),
            nt("Pat Smith*", "s26-05")),
        result);
  }

  @Test
  public void nameAndTeams_uses_first_name_only_when_unique() {
    List<NameAndTeam> result =
        service.nameAndTeams(
            List.of(student("ABHIJEET", "PATEL", "s26-14"), student("aiden", "SMITH", "s26-06")));
    assertEquals(List.of(nt("Abhijeet", "s26-14"), nt("Aiden", "s26-06")), result);
  }

  @Test
  public void nameAndTeams_adds_last_initial_when_first_name_is_shared() {
    List<NameAndTeam> result =
        service.nameAndTeams(
            List.of(
                student("Alex", "Lee", "s26-03"),
                student("Alex", "Young", "s26-10"),
                student("Alexander", "Hamilton", "s26-07")));
    assertEquals(
        List.of(nt("Alex L", "s26-03"), nt("Alex Y", "s26-10"), nt("Alexander", "s26-07")), result);
  }

  @Test
  public void nameAndTeams_uses_full_last_name_when_initial_is_shared() {
    List<NameAndTeam> result =
        service.nameAndTeams(
            List.of(
                student("David", "Chang", "s26-07"),
                student("David", "Chen", "s26-02"),
                student("David", "Smith", "s26-09")));
    assertEquals(
        List.of(nt("David Chang", "s26-07"), nt("David Chen", "s26-02"), nt("David S", "s26-09")),
        result);
  }

  @Test
  public void nameAndTeams_marks_exact_duplicates_with_asterisk() {
    List<NameAndTeam> result =
        service.nameAndTeams(
            List.of(
                student("Chris", "Gaucho", "s26-01"),
                student("CHRIS", "GAUCHO", "s26-02"),
                student("Chris", "Garcia", "s26-03")));
    assertEquals(
        List.of(
            nt("Chris Gaucho*", "s26-01"),
            nt("Chris Gaucho*", "s26-02"),
            nt("Chris Garcia", "s26-03")),
        result);
  }

  @Test
  public void nameAndTeams_handles_missing_last_names() {
    List<NameAndTeam> result =
        service.nameAndTeams(
            List.of(
                student("Pat", null, "s26-01"),
                student("Pat", "", "s26-02"),
                student("Pat", "Lee", "s26-03"),
                student("Sam", null, "s26-04")));
    assertEquals(
        List.of(
            nt("Pat*", "s26-01"), nt("Pat*", "s26-02"), nt("Pat L", "s26-03"), nt("Sam", "s26-04")),
        result);
  }

  @Test
  public void nameAndTeams_handles_missing_first_names() {
    List<NameAndTeam> result =
        service.nameAndTeams(
            List.of(
                student(null, "Lee", "s26-01"),
                student("", "Lam", "s26-02"),
                student(null, null, "s26-03")));
    assertEquals(List.of(nt("Lee", "s26-01"), nt("Lam", "s26-02"), nt("", "s26-03")), result);
  }

  @Test
  public void nameAndTeams_uses_first_team_or_blank() {
    RosterStudent nullTeams =
        RosterStudent.builder().firstName("Nora").lastName("Null").teamMembers(null).build();
    List<NameAndTeam> result =
        service.nameAndTeams(
            List.of(
                student("Max", "Many", "s26-01", "s26-02"), student("Nell", "None"), nullTeams));
    assertEquals(List.of(nt("Max", "s26-01"), nt("Nell", ""), nt("Nora", "")), result);
  }

  @Test
  public void nameAndTeams_of_empty_list_is_empty() {
    assertEquals(List.of(), service.nameAndTeams(List.of()));
  }

  // layoutColumns

  @Test
  public void layoutColumns_rejects_less_than_one_column() {
    List<String> items = List.of("A");
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> service.layoutColumns(items, 0));
    assertEquals("columns must be at least 1", e.getMessage());
    assertThrows(IllegalArgumentException.class, () -> service.layoutColumns(items, -1));
  }

  @Test
  public void layoutColumns_single_column_keeps_everything_together() {
    List<String> items = List.of("A", "B", "C");
    assertEquals(List.of(items), service.layoutColumns(items, 1));
  }

  @Test
  public void layoutColumns_divides_evenly_when_possible() {
    assertEquals(
        List.of(List.of("A", "B"), List.of("C", "D")),
        service.layoutColumns(List.of("A", "B", "C", "D"), 2));
  }

  @Test
  public void layoutColumns_gives_extra_items_to_the_first_columns() {
    assertEquals(
        List.of(List.of("A", "B"), List.of("C", "D"), List.of("E")),
        service.layoutColumns(List.of("A", "B", "C", "D", "E"), 3));
  }

  @Test
  public void layoutColumns_with_more_columns_than_items_leaves_columns_empty() {
    assertEquals(
        List.of(List.of("A"), List.of("B"), List.of()),
        service.layoutColumns(List.of("A", "B"), 3));
    assertEquals(List.of(List.of(), List.of()), service.layoutColumns(List.of(), 2));
  }

  // writeColumnsCsv

  private String columnsCsv(List<String> header, List<List<List<String>>> columns)
      throws Exception {
    StringWriter writer = new StringWriter();
    service.writeColumnsCsv(writer, header, columns);
    return writer.toString().replace("\r\n", "\n");
  }

  @Test
  public void writeColumnsCsv_pads_shorter_columns_with_blank_cells() throws Exception {
    String expected =
        """
        Team,Name,,Team,Name,,Team,Name
        s26-01,Andy,,s26-02,Kai,,,
        s26-01,Oscar,,,,,,
        """;
    List<List<List<String>>> columns =
        List.of(
            List.of(List.of("s26-01", "Andy"), List.of("s26-01", "Oscar")),
            List.of(List.of("s26-02", "Kai")),
            List.of());
    assertEquals(expected, columnsCsv(List.of("Team", "Name"), columns));
  }

  @Test
  public void writeColumnsCsv_longest_column_need_not_be_first() throws Exception {
    // Commons CSV quotes a leading empty cell so the line cannot be mistaken for an empty line.
    String expected = """
        X,,X
        "",,a
        "",,b
        """;
    List<List<List<String>>> columns = List.of(List.of(), List.of(List.of("a"), List.of("b")));
    assertEquals(expected, columnsCsv(List.of("X"), columns));
  }

  @Test
  public void writeColumnsCsv_with_no_columns_writes_an_empty_header_line() throws Exception {
    assertEquals("\n", columnsCsv(List.of("Name", "Team"), List.of()));
  }

  // writeName2TeamCsv

  private String name2TeamCsv(List<NameAndTeam> entries, int columns) throws Exception {
    StringWriter writer = new StringWriter();
    service.writeName2TeamCsv(writer, entries, columns);
    return writer.toString().replace("\r\n", "\n");
  }

  @Test
  public void writeName2TeamCsv_single_column() throws Exception {
    String expected =
        """
        Name,Team
        Abhijeet,s26-14
        Aiden,s26-06
        Alex L,s26-03
        """;
    assertEquals(
        expected,
        name2TeamCsv(
            List.of(nt("Abhijeet", "s26-14"), nt("Aiden", "s26-06"), nt("Alex L", "s26-03")), 1));
  }

  @Test
  public void writeName2TeamCsv_multiple_columns_with_blank_separator_and_short_last_column()
      throws Exception {
    String expected =
        """
        Name,Team,,Name,Team,,Name,Team
        A,1,,C,3,,E,5
        B,2,,D,4,,,
        """;
    assertEquals(
        expected,
        name2TeamCsv(
            List.of(nt("A", "1"), nt("B", "2"), nt("C", "3"), nt("D", "4"), nt("E", "5")), 3));
  }

  @Test
  public void writeName2TeamCsv_with_no_entries_writes_only_the_header() throws Exception {
    assertEquals("Name,Team,,Name,Team\n", name2TeamCsv(List.of(), 2));
  }

  @Test
  public void writeName2TeamCsv_rejects_less_than_one_column() {
    StringWriter writer = new StringWriter();
    assertThrows(
        IllegalArgumentException.class, () -> service.writeName2TeamCsv(writer, List.of(), 0));
    assertEquals("", writer.toString());
  }

  // groupByTeam

  @Test
  public void groupByTeam_sorts_teams_by_name_and_keeps_student_order_within_a_team() {
    List<NameAndTeam> entries =
        List.of(
            nt("Andy", "s26-02"),
            nt("Binghao", "s26-01"),
            nt("Oscar", "s26-02"),
            nt("Shanqin", "S26-01"),
            nt("Whisper", "s26-03"));
    assertEquals(
        List.of(
            List.of(nt("Shanqin", "S26-01")),
            List.of(nt("Binghao", "s26-01")),
            List.of(nt("Andy", "s26-02"), nt("Oscar", "s26-02")),
            List.of(nt("Whisper", "s26-03"))),
        service.groupByTeam(entries));
  }

  @Test
  public void groupByTeam_puts_students_with_no_team_last() {
    List<NameAndTeam> entries =
        List.of(nt("Andy", ""), nt("Binghao", "s26-01"), nt("Oscar", ""), nt("Zed", "a-team"));
    assertEquals(
        List.of(
            List.of(nt("Zed", "a-team")),
            List.of(nt("Binghao", "s26-01")),
            List.of(nt("Andy", ""), nt("Oscar", ""))),
        service.groupByTeam(entries));
  }

  @Test
  public void groupByTeam_of_empty_list_is_empty() {
    assertEquals(List.of(), service.groupByTeam(List.of()));
  }

  // writeTeamTableCsv

  private String teamTableCsv(List<NameAndTeam> entries, int columns) throws Exception {
    StringWriter writer = new StringWriter();
    service.writeTeamTableCsv(writer, entries, columns);
    return writer.toString().replace("\r\n", "\n");
  }

  @Test
  public void writeTeamTableCsv_balances_columns_by_team_and_never_splits_a_team()
      throws Exception {
    // 5 teams over 2 columns: 3 teams (5 students) in the first, 2 teams (5 students) in the
    // second, so the second column's leading blank cell is quoted by Commons CSV.
    List<NameAndTeam> entries =
        List.of(
            nt("Andy", "s26-01"),
            nt("Binghao", "s26-01"),
            nt("Chris", "s26-02"),
            nt("Dana", "s26-03"),
            nt("Erik", "s26-03"),
            nt("Fay", "s26-04"),
            nt("Gus", "s26-04"),
            nt("Hal", "s26-04"),
            nt("Ivy", "s26-04"),
            nt("Jo", "s26-05"));
    String expected =
        """
        Team,Name,,Team,Name
        s26-01,Andy,,s26-04,Fay
        s26-01,Binghao,,s26-04,Gus
        s26-02,Chris,,s26-04,Hal
        s26-03,Dana,,s26-04,Ivy
        s26-03,Erik,,s26-05,Jo
        """;
    assertEquals(expected, teamTableCsv(entries, 2));
  }

  @Test
  public void writeTeamTableCsv_pads_short_columns_and_quotes_leading_blank_cells()
      throws Exception {
    List<NameAndTeam> entries =
        List.of(nt("Andy", "s26-01"), nt("Binghao", "s26-02"), nt("Chris", "s26-02"));
    String expected =
        """
        Team,Name,,Team,Name,,Team,Name
        s26-01,Andy,,s26-02,Binghao,,,
        "",,,s26-02,Chris,,,
        """;
    assertEquals(expected, teamTableCsv(entries, 3));
  }

  @Test
  public void writeTeamTableCsv_single_column_lists_unassigned_students_last() throws Exception {
    List<NameAndTeam> entries =
        List.of(nt("Andy", ""), nt("Binghao", "s26-02"), nt("Chris", "s26-01"));
    String expected =
        """
        Team,Name
        s26-01,Chris
        s26-02,Binghao
        "",Andy
        """;
    assertEquals(expected, teamTableCsv(entries, 1));
  }

  @Test
  public void writeTeamTableCsv_with_no_entries_writes_only_the_header() throws Exception {
    assertEquals("Team,Name,,Team,Name\n", teamTableCsv(List.of(), 2));
  }

  @Test
  public void writeTeamTableCsv_rejects_less_than_one_column() {
    StringWriter writer = new StringWriter();
    assertThrows(
        IllegalArgumentException.class, () -> service.writeTeamTableCsv(writer, List.of(), 0));
    assertEquals("", writer.toString());
  }
}
