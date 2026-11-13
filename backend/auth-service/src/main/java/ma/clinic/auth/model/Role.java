package ma.clinic.auth.model;

public enum Role {
    DOCTOR,
    ASSISTANT,
    ADMIN,
    /** A patient using the patient portal. Created by the assistant, never by the administrator. */
    PATIENT
}
