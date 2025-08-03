package edu.ucsb.cs156.frontiers.models;



import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByNames;
import lombok.*;

@Data
public class PreConvertRosterStudent {

    @CsvBindByName(column = "Student SIS ID")
    public String studentId;


    @CsvBindByName(column = "Student First Middle")
    public String firstName;

    @CsvBindByName(column = "Student Last")
    public String lastName;

    @CsvBindByName(column = "Email")
    public String email;

    @CsvBindByName(column = "Student Name")
    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    public String fullName;

    @CsvBindByName(column = "Perm #")
    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    public String perm;

    public void setFullName(String fullName) {
        try {
            this.firstName = fullName.substring(0, fullName.indexOf(" "));
            this.lastName = fullName.substring(fullName.indexOf(" ") + 1);
        } catch (IndexOutOfBoundsException e) {
            this.firstName = fullName;
            this.lastName = "";
        }
    }

    public void setPerm(String perm) {
        this.studentId = perm;
    }
}
