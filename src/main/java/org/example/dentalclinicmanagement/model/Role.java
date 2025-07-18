package org.example.dentalclinicmanagement.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.example.dentalclinicmanagement.model.Permission.*;

@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN(Set.of(
            READ, CREATE, UPDATE, DELETE,
            IMPORT_DATA, EXPORT_DATA, SYSTEM_SETTINGS,
            CALENDAR_VIEW, CALENDAR_MANAGE,
            APPOINTMENT_CREATE, APPOINTMENT_UPDATE, APPOINTMENT_DELETE, APPOINTMENT_VIEW_ALL,
            PATIENT_VIEW, PATIENT_CREATE, PATIENT_UPDATE,
            DENTIST_VIEW, DENTIST_CREATE, DENTIST_UPDATE, DENTIST_DELETE
    )),

    MANAGER(Set.of(
            READ, UPDATE,
            CALENDAR_VIEW, CALENDAR_MANAGE,
            APPOINTMENT_CREATE, APPOINTMENT_UPDATE, APPOINTMENT_VIEW_ALL,
            PATIENT_VIEW, PATIENT_UPDATE,
            DENTIST_VIEW
    )),

    DENTIST(Set.of(
            READ, UPDATE,
            CALENDAR_VIEW,
            APPOINTMENT_VIEW_ALL, APPOINTMENT_UPDATE,
            PATIENT_VIEW, PATIENT_UPDATE,
            DENTIST_VIEW
    )),

    USER(Set.of(
            APPOINTMENT_VIEW_OWN, APPOINTMENT_CANCEL_OWN,
            PATIENT_VIEW, PATIENT_UPDATE
    ));

    private final Set<Permission> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        permissions.forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.getPermission()))
        );
        return authorities;
    }
}