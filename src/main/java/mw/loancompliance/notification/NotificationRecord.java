package mw.loancompliance.notification;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import mw.loancompliance.user.entity.User;
import java.time.Instant;
@Entity @Table(name="notifications") public class NotificationRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @JsonIgnore @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") User user;
    String title;
    String message;
    String channel;
    @Column(name="delivery_status") String deliveryStatus;
    @Column(name="event_type") String eventType;
    @Column(name="read_at") Instant readAt;
    @Column(name="sent_at") Instant sentAt;
    @Column(name="created_at",insertable=false,updatable=false) Instant createdAt;
    protected NotificationRecord() {
    }
    public Long getId() {
        return id;
    }
    public User getUser() {
        return user;
    }
    public String getTitle() {
        return title;
    }
    public String getMessage() {
        return message;
    }
    public String getChannel() {
        return channel;
    }
    public String getDeliveryStatus() {
        return deliveryStatus;
    }
    public String getEventType() {
        return eventType;
    }
    public Instant getReadAt() {
        return readAt;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void markRead() {
        readAt=Instant.now();
    }
}
