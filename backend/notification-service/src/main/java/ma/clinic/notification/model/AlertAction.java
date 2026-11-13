package ma.clinic.notification.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "alert_actions")
public class AlertAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(name = "actor", nullable = false)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType type;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AlertAction() {
    }

    public AlertAction(Alert alert, String actor, ActionType type, String note) {
        this.alert = alert;
        this.actor = actor;
        this.type = type;
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Alert getAlert() { return alert; }
    public String getActor() { return actor; }
    public ActionType getType() { return type; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
