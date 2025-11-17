package com.toastedsiopao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IssueReportDto {

    @NotNull
    private Long orderId;

    @NotBlank(message = "Summary cannot be blank")
    @Size(max = 255, message = "Summary cannot exceed 255 characters")
    private String summary;

    @Size(max = 2000, message = "Details cannot exceed 2000 characters")
    private String details;
}