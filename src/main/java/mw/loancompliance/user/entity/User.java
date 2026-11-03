
package mw.loancompliance.user.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = true, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    private UserAccountStatus accountStatus;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {
        // Required by JPA to instantiate User entities.
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role == null ? null : role.name();
    }

    public void setRole(String role) {
        this.role = role == null ? null : UserRole.valueOf(role);
    }

    public String getAccountStatus() {
        return accountStatus == null ? null : accountStatus.name();
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus == null
                ? null
                : UserAccountStatus.valueOf(accountStatus);
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
     @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
