package org.example.dentalclinicmanagement.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permission {
    READ("read"),
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),

    IMPORT_DATA("import:data"),
    EXPORT_DATA("export:data"),
    SYSTEM_SETTINGS("system:settings"),

    CALENDAR_VIEW("calendar:view"),
    CALENDAR_MANAGE("calendar:manage"),
    APPOINTMENT_CREATE("appointment:create"),
    APPOINTMENT_UPDATE("appointment:update"),
    APPOINTMENT_DELETE("appointment:delete"),
    APPOINTMENT_VIEW_ALL("appointment:view:all"),
    APPOINTMENT_VIEW_OWN("appointment:view:own"),
    APPOINTMENT_CANCEL_OWN("appointment:cancel:own"),

    PATIENT_VIEW("patient:view"),
    PATIENT_CREATE("patient:create"),
    PATIENT_UPDATE("patient:update"),

    DENTIST_VIEW("dentist:view"),
    DENTIST_CREATE("dentist:create"),
    DENTIST_UPDATE("dentist:update"),
    DENTIST_DELETE("dentist:delete"),

;
    private final String permission;
}