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
    private String reportedAt; 
    private String username;   
    private String resolvedAt; 
    private String resolvedByAdmin;
    private String adminNotes;
}