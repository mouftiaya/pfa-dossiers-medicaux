package ma.clinic.auth.service;

import ma.clinic.auth.model.AuditLog;
import ma.clinic.auth.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    public void log(String username, String action, String details) {
        logs.save(new AuditLog(username, action, details));
    }
}
