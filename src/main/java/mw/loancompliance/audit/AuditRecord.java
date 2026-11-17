package mw.loancompliance.audit;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="audit_logs") public class AuditRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="actor_user_id") Long actorUserId;
    @Column(name="actor_role") String actorRole;
    String action;
    @Column(name="entity_type") String entityType;
    @Column(name="entity_id") Long entityId;
    String outcome;
    String description;
    @Column(name="created_at",insertable=false,updatable=false) Instant createdAt;
    protected AuditRecord() {
    }
    public static AuditRecord event(Long actor,String role,String action,String type,Long entity,String description) {
        AuditRecord r=new AuditRecord();
        r.actorUserId=actor;
        r.actorRole=role;
        r.action=action;
        r.entityType=type;
        r.entityId=entity;
        r.outcome="SUCCESS";
        r.description=description;
        return r;
    }
    public Long getId() {
        return id;
    }
    public Long getActorUserId() {
        return actorUserId;
    }
    public String getActorRole() {
        return actorRole;
    }
    public String getAction() {
        return action;
    }
    public String getEntityType() {
        return entityType;
    }
    public Long getEntityId() {
        return entityId;
    }
    public String getOutcome() {
        return outcome;
    }
    public String getDescription() {
        return description;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
}
