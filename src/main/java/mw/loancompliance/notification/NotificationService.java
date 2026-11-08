package mw.loancompliance.notification;
import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.repository.UserRepository;
import org.springframework.stereotype.Service;
@Service public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    public NotificationService(NotificationRepository n,UserRepository u) {
        notifications=n;
        users=u;
    }
    public void inApp(User user,String event,String title,String message) {
        NotificationRecord n=new NotificationRecord();
        n.user=user;
        n.eventType=event;
        n.title=title;
        n.message=message;
        n.channel="IN_APP";
        n.deliveryStatus="SENT";
        n.sentAt=java.time.Instant.now();
        notifications.save(n);
    }
    public void inAppOnce(User user,String event,String title,String message,String deduplicationText) {
        if (notifications.existsByUser_IdAndEventTypeAndMessageContaining(
                user.getId(), event, deduplicationText)) {
            return;
        }
        inApp(user, event, title, message);
    }
    public void inApp(Long userId,String event,String title,String message) {
        users.findById(userId).ifPresent(u->inApp(u,event,title,message));
    }
}
