package com.toastedsiopao.service;

import com.toastedsiopao.dto.IssueReportDto;
import com.toastedsiopao.dto.IssueReportResponseDto; 
import com.toastedsiopao.model.IssueReport;
import com.toastedsiopao.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface IssueReportService {

    IssueReport createIssueReport(User user, IssueReportDto reportDto, MultipartFile attachmentFile);

    List<IssueReportResponseDto> findReportsByOrderId(Long orderId); 

    Map<Long, Long> getOpenIssueCountsForOrders(List<Long> orderIds);
    
    IssueReportResponseDto resolveIssueReport(Long issueId, User admin, String adminNotes);

    boolean doesReportExistForOrder(Long orderId);
    
    IssueReportResponseDto getCustomerReportForOrder(User user, Long orderId);
}