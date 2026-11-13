package ma.clinic.notification.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import ma.clinic.notification.dto.EventDtos.AbsenceEvent;
import ma.clinic.notification.dto.EventDtos.AiRiskEvent;
import ma.clinic.notification.dto.EventDtos.GlucoseEvent;
import ma.clinic.notification.dto.EventDtos.LabCriticalEvent;
import ma.clinic.notification.service.AlertService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Events posted by the other services. Not routed by the gateway; protected by the X-Internal-Key header. */
@RestController
@RequestMapping("/internal/events")
public class InternalEventController {

    private final AlertService service;
    private final String internalKey;

    public InternalEventController(AlertService service,
                                   @Value("${internal.api-key:change-this-internal-key-in-production}") String internalKey) {
        this.service = service;
        this.internalKey = internalKey;
    }

    @PostMapping("/glucose")
    public ResponseEntity<Map<String, Integer>> glucose(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @RequestBody GlucoseEvent event) {
        checkKey(key);
        return accepted(service.handleGlucose(event));
    }

    @PostMapping("/absence")
    public ResponseEntity<Map<String, Integer>> absence(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @RequestBody AbsenceEvent event) {
        checkKey(key);
        return accepted(service.handleAbsence(event));
    }

    @PostMapping("/lab-critical")
    public ResponseEntity<Map<String, Integer>> labCritical(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @RequestBody LabCriticalEvent event) {
        checkKey(key);
        return accepted(service.handleLabCritical(event));
    }

    /** Posted by ai-service. Answer: {"result": "created"} or {"result": "duplicate"}. */
    @PostMapping("/ai-risk")
    public ResponseEntity<Map<String, String>> aiRisk(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @RequestBody AiRiskEvent event) {
        checkKey(key);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("result", service.handleAiRisk(event)));
    }

    private static ResponseEntity<Map<String, Integer>> accepted(int created) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("alertsCreated", created));
    }

    /** Constant-time comparison of the header with the configured key. */
    private void checkKey(String provided) {
        boolean ok = provided != null && MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8), internalKey.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal key");
        }
    }
}
