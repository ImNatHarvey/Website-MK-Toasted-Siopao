package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; 

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private Clock clock; 

	@Override
	@Transactional(readOnly = true)
	public Optional<Order> findOrderById(Long id) {
		return orderRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Order> findOrdersByUser(User user, Pageable pageable) { 
		return orderRepository.findByUserOrderByOrderDateDesc(user, pageable); 
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Order> findAllOrders(Pageable pageable) { 
		return orderRepository.findAllByDate(null, null, pageable); 
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Order> findOrdersByStatus(String status, Pageable pageable) { 
		if (!StringUtils.hasText(status)) {
			return findAllOrders(pageable); 
		}
		return orderRepository.searchByStatusAndDate(status.toUpperCase(), null, null, pageable); 
	}
	
	@Override
	@Transactional(readOnly = true)
	public Page<Order> searchOrders(String keyword, String status, String startDate, String endDate,
			Pageable pageable) { 
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasStatus = StringUtils.hasText(status);
		boolean hasStartDate = StringUtils.hasText(startDate);
		boolean hasEndDate = StringUtils.hasText(endDate);

		LocalDateTime startDateTime = null;
		LocalDateTime endDateTime = null;

		try {
			if (hasStartDate) {
				startDateTime = LocalDate.parse(startDate).atStartOfDay();
			}
		} catch (Exception e) {
			log.warn("Invalid start date format: {}. Ignoring.", startDate);
		}

		try {
			if (hasEndDate) {
				endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
			}
		} catch (Exception e) {
			log.warn("Invalid end date format: {}. Ignoring.", endDate);
		}

		String upperStatus = hasStatus ? status.toUpperCase() : null;

		if (hasKeyword && hasStatus) {
			return orderRepository.searchOrdersByKeywordAndStatus(keyword.trim(), upperStatus, startDateTime,
					endDateTime, pageable); 
		} else if (hasKeyword) {
			return orderRepository.searchOrdersByKeyword(keyword.trim(), startDateTime, endDateTime, pageable);
		} else if (hasStatus) {
			return orderRepository.searchByStatusAndDate(upperStatus, startDateTime, endDateTime, pageable); 
		} else {
			return orderRepository.findAllByDate(startDateTime, endDateTime, pageable); 
		}
	}
	
	@Override
	@Transactional(readOnly = true)
	public BigDecimal getSalesToday() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfDay = now.with(LocalTime.MIN);
		return orderRepository.findTotalSalesBetweenDates(startOfDay, now);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getSalesThisWeek() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
		return orderRepository.findTotalSalesBetweenDates(startOfWeek, now);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getSalesThisMonth() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
		return orderRepository.findTotalSalesBetweenDates(startOfMonth, now);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Long> getOrderStatusCounts() {
		return orderRepository.countOrdersByStatus().stream().collect(Collectors.toMap(row -> (String) row[0],
				row -> (Long) row[1] 
		));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getTopSellingProducts(int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		Page<Object[]> results = orderRepository.findTopSellingProducts(pageable);

		return results.getContent().stream()
				.map(row -> Map.<String, Object>of("product", (Product) row[0], "quantity", (Long) row[1]))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, BigDecimal> getSalesDataForChart(LocalDateTime start, LocalDateTime end) {
		List<Object[]> results = orderRepository.findSalesDataBetweenDates(start, end);

		return results.stream()
				.collect(Collectors.toMap(row -> ((Date) row[0]).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
						row -> (BigDecimal) row[1], 
						(oldValue, newValue) -> oldValue.add(newValue),
						LinkedHashMap::new));
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getTotalRevenueAllTime() {
		BigDecimal total = orderRepository.findTotalRevenueAllTime();
		return total != null ? total : BigDecimal.ZERO;
	}

	@Override
	@Transactional(readOnly = true)
	public long getTotalTransactionsAllTime() {
		return orderRepository.countTotalTransactionsAllTime();
	}
}