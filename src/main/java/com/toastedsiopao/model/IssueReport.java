package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "issue_reports")
@Data
@NoArgsConstructor
public class IssueReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Summary cannot be blank")
    @Size(max = 255, message = "Summary cannot exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String summary;

    @Column(length = 2000)
    private String details;

    @Column(length = 255)
    private String attachmentImageUrl;

    @Column(nullable = false)
    private boolean isOpen = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reportedAt;
    
    // --- START: NEW FIELDS ---
    @Column(nullable = true)
    private LocalDateTime resolvedAt;
    
    @Column(nullable = true, length = 100)
    private String resolvedByAdmin; // Stores the username of the admin who resolved it
    
    @Column(length = 1000)
    private String adminNotes; // Optional notes from the admin
    // --- END: NEW FIELDS ---

    @PrePersist
    protected void onCreate() {
        reportedAt = LocalDateTime.now();
    }

    public IssueReport(Order order, User user, String summary, String details, String attachmentImageUrl) {
        this.order = order;
        this.user = user;
        this.summary = summary;
        this.details = details;
        this.attachmentImageUrl = attachmentImageUrl;
        this.isOpen = true;
    }
}