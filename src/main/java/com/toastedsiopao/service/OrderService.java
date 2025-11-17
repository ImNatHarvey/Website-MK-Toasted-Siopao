package com.toastedsiopao.service;

import com.toastedsiopao.dto.OrderSubmitDto; 
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; 

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {

	Order createOrder(User user, OrderSubmitDto orderDto, String receiptImagePath);

	Optional<Order> findOrderById(Long id);

	Page<Order> findOrdersByUser(User user, Pageable pageable); 

	Page<Order> findAllOrders(Pageable pageable); 
	
	Page<Order> findOrdersByStatus(String status, Pageable pageable); 
	
	Page<Order> searchOrders(String keyword, String status, String startDate, String endDate, Pageable pageable); 

	Page<Order> findOrdersByUserAndStatus(User user, String status, Pageable pageable); 

	Order cancelOrder(Long orderId, User customer);
	Order acceptOrder(Long orderId);
	Order rejectOrder(Long orderId);
	Order shipOrder(Long orderId);
	Order completeCodOrder(Long orderId);
	Order completeDeliveredOrder(Long orderId);

	BigDecimal getSalesToday();

	BigDecimal getSalesThisWeek();

	BigDecimal getSalesThisMonth();

	Map<String, Long> getOrderStatusCounts();

	List<Map<String, Object>> getTopSellingProducts(int limit);

	Map<String, BigDecimal> getSalesDataForChart(LocalDateTime start, LocalDateTime end);

	BigDecimal getTotalRevenueAllTime();

	long getTotalTransactionsAllTime();

	BigDecimal getTotalPotentialRevenue();

	BigDecimal getEstimatedCogsBetweenDates(LocalDateTime start, LocalDateTime end);
	
	BigDecimal getCogsToday();

	BigDecimal getCogsThisWeek();

	BigDecimal getCogsThisMonth();
	
	List<Order> findDeliveredOrdersForReport(String keyword, LocalDateTime start, LocalDateTime end);

	BigDecimal calculateCogsForOrder(Order order);

	Optional<Order> findOrderForInvoice(Long orderId);
}