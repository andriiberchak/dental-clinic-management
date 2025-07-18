package org.example.dentalclinicmanagement.security.oauth2;

public interface OAuth2UserInfo {
    String getId();
    String getFirstName();
    String getLastName();
    String getEmail();
}