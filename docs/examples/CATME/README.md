# CATME example files

All of the files in this folder contain **entirely fictitious** data for a made-up
course, CMPSC 123, Fall 2026. The four files describe the same 24 students in two
sections and six teams of four.

| File | Format | Purpose |
|------|--------|---------|
| `CMPSC123-Fall-2026-Fake-Egrades.csv` | UCSB eGrades roster | Upload this first, as the course roster. |
| `CMPSC123-Fall-2026-Fake-Teams.csv` | Frontiers team upload (`team,email`) | Puts the 24 students on teams `cmpsc123-01` through `cmpsc123-06`. |
| `FakeInstructor-TeamFormation-CMPSC-123-Fall-2026-matches.csv` | CATME TeamFormation export | Agrees exactly with the two files above: same students, IDs, emails, sections and teams. |
| `FakeInstructor-TeamFormation-CMPSC-123-Fall-2026-mismatches.csv` | CATME TeamFormation export | Deliberately disagrees with the two files above (see below). |

## About the CATME format

The CATME files copy the layout of a real CATME TeamFormation export. That layout is
not a conventional CSV file, even though it has a `.csv` extension. It has several
sections, each with its own header row, separated by blank lines:

1. Lines 1–2: activity information (`Activity,Class,Term,Format,Instr,School`).
2. Line 3: blank.
3. Line 4 onward: survey responses, one row per student
   (`"Name","Student ID","Email","Section",...` with a trailing comma).
4. Two blank lines, then an empty `"Name","Student ID","Email","Section",` table.
5. `"Rater Practice"`: one row per student, giving the student's **team** in the
   second column. In this layout, this is the only place where team assignments appear.
6. A blank line, then `"Comments"`: one row per student. A comment may span several
   lines inside a quoted field.

Lines end with CRLF, as in the real export.

The `Section` column in the CATME files holds the eGrades **Enrl Cd** (`71530` or
`71548`), which is what Frontiers stores as a roster student's section. So the files
agree without the Translate Sections course option.

## Differences in the mismatches file

Compared with the eGrades roster and the teams CSV, the mismatches file has:

* **3 students in CATME who are not in eGrades:**
  `PEMBERWICK, GWENDOLYN`, `SALTONSTALL, CORNELIUS`, `YARBOROUGHE, MARISOL`
* **3 students in both files but on the wrong team in CATME:**
  * `QUILLFEATHER, AMARA JUNE` is on `cmpsc123-02` in CATME but `cmpsc123-01` in the teams CSV.
  * `MARLOWE-PIKE, CASPIAN LOCKE` is on `cmpsc123-03` in CATME but `cmpsc123-02` in the teams CSV.
  * `HOLLISTRAND, SORREL` is on `cmpsc123-01` in CATME but `cmpsc123-03` in the teams CSV.
* **3 students in eGrades who are not in CATME:**
  `FENNIMORE, ROSALIND IVY`, `MOONSHADOW, PERPETUA`, `WREXHAMPTON, EZEKIEL`

Names, student IDs, emails and sections match for every student who appears in both files.
