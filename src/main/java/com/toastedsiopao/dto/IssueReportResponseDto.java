package com.toastedsiopao.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IssueReportResponseDto {
    private Long id;
    private String summary;
    private String details;
    private String attachmentImageUrl;
    private boolean isOpen;
    private String reportedAt; // Formatted as String
    private String username;   // Flattened from user.username

    // Resolution fields
    private String resolvedAt; // Formatted as String
    private String resolvedByAdmin;
    private String adminNotes;
}