package com.toastedsiopao.repository;

import com.toastedsiopao.model.ActivityLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntry, Long> {

	// --- EXISTING WASTE FILTER ---
	String WASTE_FILTER_CONDITION = "(a.action LIKE 'STOCK_WASTE_%' OR a.action LIKE 'PRODUCT_WASTE_%') "
			+ "AND (:typeFilter IS NULL OR (:typeFilter = 'INVENTORY' AND a.action LIKE 'STOCK_WASTE_%') OR (:typeFilter = 'PRODUCT' AND a.action LIKE 'PRODUCT_WASTE_%')) "
			+ "AND (:reasonSuffix IS NULL OR a.action LIKE CONCAT('%_', :reasonSuffix)) "
			+ "AND (:itemName IS NULL OR LOWER(a.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))) "
			+ "AND (:startDate IS NULL OR a.timestamp >= :startDate) "
			+ "AND (:endDate IS NULL OR a.timestamp <= :endDate)";

	// --- NEW: GENERAL ACTIVITY LOG FILTER ---
	String LOG_FILTER_CONDITION = "(:keyword IS NULL OR "
			+ "LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(a.action) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(a.details) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
			+ "AND (:startDate IS NULL OR a.timestamp >= :startDate) "
			+ "AND (:endDate IS NULL OR a.timestamp <= :endDate)";
	// --- END NEW ---

	Page<ActivityLogEntry> findAllByOrderByTimestampDesc(Pageable pageable);

	Page<ActivityLogEntry> findByActionStartingWithOrderByTimestampDesc(String actionPrefix, Pageable pageable);

	Page<ActivityLogEntry> findByActionStartingWithAndDetailsContainingIgnoreCaseOrderByTimestampDesc(
			String actionPrefix, String detailsKeyword, Pageable pageable);

	@Query("SELECT a FROM ActivityLogEntry a WHERE " + WASTE_FILTER_CONDITION + " ORDER BY a.timestamp DESC")
	Page<ActivityLogEntry> searchWasteLogs(@Param("typeFilter") String typeFilter,
			@Param("reasonSuffix") String reasonSuffix, @Param("itemName") String itemName,
			@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

	// --- NEW: SEARCH METHOD FOR GENERAL LOGS ---
	@Query("SELECT a FROM ActivityLogEntry a WHERE " + LOG_FILTER_CONDITION + " ORDER BY a.timestamp DESC")
	Page<ActivityLogEntry> searchLogs(@Param("keyword") String keyword, @Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate, Pageable pageable);
	// --- END NEW ---

	// --- DYNAMIC METRICS (Respecting Filters) ---

	@Query("SELECT COUNT(a) FROM ActivityLogEntry a WHERE " + WASTE_FILTER_CONDITION)
	long countFilteredWaste(@Param("typeFilter") String typeFilter, @Param("reasonSuffix") String reasonSuffix,
			@Param("itemName") String itemName, @Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT COALESCE(SUM(a.totalValue), 0) FROM ActivityLogEntry a WHERE " + WASTE_FILTER_CONDITION)
	BigDecimal sumFilteredWasteValue(@Param("typeFilter") String typeFilter, @Param("reasonSuffix") String reasonSuffix,
			@Param("itemName") String itemName, @Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	// Calculates sum for a specific reason (e.g., EXPIRED) *within* the current
	// filter context
	@Query("SELECT COALESCE(SUM(a.totalValue), 0) FROM ActivityLogEntry a WHERE " + WASTE_FILTER_CONDITION
			+ " AND a.action LIKE CONCAT('%_', :specificReason)")
	BigDecimal sumFilteredWasteValueByReason(@Param("typeFilter") String typeFilter,
			@Param("reasonSuffix") String reasonSuffix, @Param("itemName") String itemName,
			@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
			@Param("specificReason") String specificReason);

	// --- GLOBAL METRICS (For reports/unfiltered view if needed, kept for backward
	// compatibility) ---
	@Query("SELECT COALESCE(SUM(a.totalValue), 0) FROM ActivityLogEntry a WHERE a.action LIKE '%_WASTE_%'")
	BigDecimal sumTotalWasteValue();

	@Query("SELECT COUNT(a) FROM ActivityLogEntry a WHERE a.action LIKE '%_WASTE_%'")
	long countTotalWasteEntries();

	@Query("SELECT COALESCE(SUM(a.totalValue), 0) FROM ActivityLogEntry a WHERE a.action LIKE CONCAT('%_WASTE_', :reasonSuffix)")
	BigDecimal sumValueByReason(@Param("reasonSuffix") String reasonSuffix);
}