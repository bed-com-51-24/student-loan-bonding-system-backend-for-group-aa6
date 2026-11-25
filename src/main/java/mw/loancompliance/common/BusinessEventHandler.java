package mw.loancompliance.common;

import mw.loancompliance.audit.AuditService;
import mw.loancompliance.notification.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BusinessEventHandler {
    private final AuditService audit;
    private final NotificationService notifications;

    public BusinessEventHandler(AuditService audit, NotificationService notifications) {
        this.audit = audit;
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(BusinessEvent event) {
        audit.record(event.actorEmail(), event.action(), event.entityType(),
                event.entityId(), event.description());
        if (event.notificationUserId() != null) {
            notifications.inApp(event.notificationUserId(), event.action(),
                    event.notificationTitle(), event.notificationMessage());
        }
    }
}
