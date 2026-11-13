package ma.clinic.notification.service;

/**
 * Suggested e-mail for a patient whose glucose has been out of range for weeks. The assistant can edit it before
 * sending. The default text stays neutral on purpose (no value, no diagnosis: an e-mail can be read by someone else,
 * cf. the cahier des charges) and invites the patient to book an appointment with the doctor.
 */
public final class EmailDraftBuilder {

    private EmailDraftBuilder() {
    }

    public static String subject(String clinicName) {
        return clinicName + " : merci de prendre rendez-vous";
    }

    public static String body(String clinicName, String clinicPhone, String firstName) {
        String greeting = firstName == null || firstName.isBlank() ? "Bonjour," : "Bonjour " + firstName.trim() + ",";
        return greeting + "\n\n"
                + "Suite au suivi de vos données ces dernières semaines, nous vous invitons à prendre rendez-vous "
                + "avec le médecin dès que possible afin de faire le point sur votre suivi et de retrouver un meilleur "
                + "équilibre.\n\n"
                + "Vous pouvez nous joindre au " + clinicPhone + " pour fixer un créneau.\n\n"
                + "Cordialement,\n" + clinicName;
    }
}
