package ma.clinic.auth.controller;

import ma.clinic.auth.model.AuditLog;
import ma.clinic.auth.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Activity journal, reserved to the administrator (see SecurityConfig). */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditLogRepository logs;

    public AuditController(AuditLogRepository logs) {
        this.logs = logs;
    }

    /** Most recent first, 50 per page by default (max 200). */
    @GetMapping
    public Page<AuditLog> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        return logs.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(Math.max(page, 0), safeSize));
    }
}
