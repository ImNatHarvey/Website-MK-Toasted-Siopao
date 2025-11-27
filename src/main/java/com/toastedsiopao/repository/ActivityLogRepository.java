package com.toastedsiopao.repository;

import com.toastedsiopao.model.ActivityLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntry, Long> {

	Page<ActivityLogEntry> findAllByOrderByTimestampDesc(Pageable pageable);

	Page<ActivityLogEntry> findByActionStartingWithOrderByTimestampDesc(String actionPrefix, Pageable pageable);
	
	Page<ActivityLogEntry> findByActionStartingWithAndDetailsContainingIgnoreCaseOrderByTimestampDesc(
			String actionPrefix, String detailsKeyword, Pageable pageable);
	
	// --- MODIFIED: Unified search for Inventory and Product waste with Filters ---
	@Query("SELECT a FROM ActivityLogEntry a WHERE " + 
	       "(a.action LIKE 'STOCK_WASTE_%' OR a.action LIKE 'PRODUCT_WASTE_%') " +
	       "AND (:typeFilter IS NULL OR (:typeFilter = 'INVENTORY' AND a.action LIKE 'STOCK_WASTE_%') OR (:typeFilter = 'PRODUCT' AND a.action LIKE 'PRODUCT_WASTE_%')) " +
	       "AND (:reasonSuffix IS NULL OR a.action LIKE CONCAT('%_', :reasonSuffix)) " +
	       "AND (:itemName IS NULL OR LOWER(a.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))) " +
	       "ORDER BY a.timestamp DESC")
	Page<ActivityLogEntry> searchWasteLogs(@Param("typeFilter") String typeFilter,
	                                       @Param("reasonSuffix") String reasonSuffix,
	                                       @Param("itemName") String itemName, 
	                                       Pageable pageable);

	// --- METRICS ---
	@Query("SELECT COALESCE(SUM(a.totalValue), 0) FROM ActivityLogEntry a WHERE a.action LIKE '%_WASTE_%'")
	BigDecimal sumTotalWasteValue();

	@Query("SELECT COUNT(a) FROM ActivityLogEntry a WHERE a.action LIKE '%_WASTE_%'")
	long countTotalWasteEntries();

	@Query("SELECT COALESCE(SUM(a.totalValue), 0) FROM ActivityLogEntry a WHERE a.action LIKE CONCAT('%_WASTE_', :reasonSuffix)")
	BigDecimal sumValueByReason(@Param("reasonSuffix") String reasonSuffix);
}