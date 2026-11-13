package ma.clinic.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ma.clinic.notification.model.AlertSeverity;
import org.junit.jupiter.api.Test;

class OutreachTextTest {

    @Test
    void emailGreetsThePatientAndGivesThePhone() {
        String body = EmailDraftBuilder.body("Cabinet X", "05 11 22 33 44", " Aya ");
        assertTrue(body.startsWith("Bonjour Aya,"));
        assertTrue(body.contains("05 11 22 33 44"));
        assertTrue(body.contains("rendez-vous"));
    }

    @Test
    void emailWithoutNameAndWithoutMedicalDetails() {
        String body = EmailDraftBuilder.body("Cabinet X", "05", null);
        assertTrue(body.startsWith("Bonjour,"));
        assertFalse(body.toLowerCase().contains("mg/dl"));
        assertFalse(body.toLowerCase().contains("diab"));
    }

    @Test
    void aiSeverityDependsOnScore() {
        assertEquals(AlertSeverity.WARNING, AlertService.aiSeverity(79));
        assertEquals(AlertSeverity.CRITICAL, AlertService.aiSeverity(80));
    }
}
