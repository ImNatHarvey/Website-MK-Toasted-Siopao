package com.toastedsiopao.repository;

import com.toastedsiopao.model.IssueReport;
import com.toastedsiopao.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface IssueReportRepository extends JpaRepository<IssueReport, Long> {

    List<IssueReport> findByOrder(Order order);
    
    boolean existsByOrder(Order order); // --- ADDED ---

    List<IssueReport> findByOrderAndIsOpenTrue(Order order);

    @Query("SELECT r.order.id, COUNT(r) FROM IssueReport r WHERE r.isOpen = true AND r.order.id IN :orderIds GROUP BY r.order.id")
    Map<Long, Long> findOpenIssueCountsByOrderIds(@Param("orderIds") List<Long> orderIds);
    
    @Query("SELECT r FROM IssueReport r WHERE r.order.id = :orderId ORDER BY r.reportedAt DESC")
    List<IssueReport> findByOrderId(@Param("orderId") Long orderId);
}