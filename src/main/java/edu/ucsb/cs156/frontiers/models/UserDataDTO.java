package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDataDTO {
    private long id;
    private String email;
    private String googleSub;
    private String pictureUrl;
    private String fullName;
    private String givenName;
    private String familyName;
    private boolean admin;
    private boolean instructor;

    public static UserDataDTO from(User user, boolean isAdmin, boolean isInstructor) {
        UserDataDTO dto = UserDataDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .googleSub(user.getGoogleSub())
                .pictureUrl(user.getPictureUrl())
                .fullName(user.getFullName())
                .givenName(user.getGivenName())
                .familyName(user.getFamilyName())
                .admin(isAdmin)
                .instructor(isInstructor)
                .build();
        return dto;
    }
}
