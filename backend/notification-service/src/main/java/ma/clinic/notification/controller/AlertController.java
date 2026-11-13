package ma.clinic.notification.controller;

import java.util.List;

import jakarta.validation.Valid;
import ma.clinic.notification.dto.AlertDtos.ActionRequest;
import ma.clinic.notification.dto.AlertDtos.AlertDetailDto;
import ma.clinic.notification.dto.AlertDtos.AlertDto;
import ma.clinic.notification.dto.AlertDtos.EmailDraftDto;
import ma.clinic.notification.dto.AlertDtos.SendEmailRequest;
import ma.clinic.notification.dto.AlertDtos.SummaryDto;
import ma.clinic.notification.model.AlertSeverity;
import ma.clinic.notification.model.AlertStatus;
import ma.clinic.notification.service.AlertService;
import ma.clinic.notification.service.OutreachService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService service;
    private final OutreachService outreach;

    public AlertController(AlertService service, OutreachService outreach) {
        this.service = service;
        this.outreach = outreach;
    }

    @GetMapping
    public List<AlertDto> list(@RequestParam(required = false) AlertStatus status,
                               @RequestParam(required = false) Long patientId,
                               @RequestParam(required = false) AlertSeverity severity,
                               Authentication authentication) {
        return service.list(status, patientId, severity, canSeeDetail(authentication));
    }

    @GetMapping("/summary")
    public SummaryDto summary(@RequestParam(required = false) Long patientId) {
        return service.summary(patientId);
    }

    @GetMapping("/{id}")
    public AlertDetailDto detail(@PathVariable Long id, Authentication authentication) {
        return service.detail(id, canSeeDetail(authentication));
    }

    /** Assistants only (SecurityConfig): the actor is the subject of the token. */
    @PostMapping("/{id}/actions")
    public AlertDetailDto addAction(@PathVariable Long id, @Valid @RequestBody ActionRequest request,
                                    @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return service.addAction(id, request, jwt.getSubject(), canSeeDetail(authentication));
    }

    /** Assistants only (SecurityConfig): the e-mail she can edit and send to the patient. */
    @GetMapping("/{id}/email-draft")
    public EmailDraftDto emailDraft(@PathVariable Long id) {
        return outreach.draft(id);
    }

    /** Assistants only (SecurityConfig): sends the (edited) e-mail and records the action "contacted". */
    @PostMapping("/{id}/send-email")
    public AlertDetailDto sendEmail(@PathVariable Long id, @Valid @RequestBody SendEmailRequest request,
                                    @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return outreach.sendEmail(id, request, jwt.getSubject(), canSeeDetail(authentication));
    }

    /** Privacy rule: a doctor never receives the detail text (it can contain individual values). */
    static boolean canSeeDetail(Authentication authentication) {
        return authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_DOCTOR".equals(a.getAuthority()));
    }
}
