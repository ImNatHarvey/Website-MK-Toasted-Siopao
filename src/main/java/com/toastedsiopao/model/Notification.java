package com.toastedsiopao.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Nullable, as admin notifications aren't tied to one user
    private User user;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 255)
    private String link;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private boolean isForAdmin = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Notification(User user, String message, String link, boolean isForAdmin) {
        this.user = user;
        this.message = message;
        this.link = link;
        this.isForAdmin = isForAdmin;
    }
}