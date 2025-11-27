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

	// Efficiently fetch logs where action starts with a prefix (e.g., "STOCK_WASTE_")
	Page<ActivityLogEntry> findByActionStartingWithOrderByTimestampDesc(String actionPrefix, Pageable pageable);
	
	Page<ActivityLogEntry> findByActionStartingWithAndDetailsContainingIgnoreCaseOrderByTimestampDesc(
			String actionPrefix, String detailsKeyword, Pageable pageable);
	
	// --- ADDED: New search method supporting Item Name search (using new field) ---
	@Query("SELECT a FROM ActivityLogEntry a WHERE a.action LIKE CONCAT(:actionPrefix, '%') " +
	       "AND (:itemName IS NULL OR LOWER(a.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))) " +
	       "ORDER BY a.timestamp DESC")
	Page<ActivityLogEntry> searchWasteLogs(@Param("actionPrefix") String actionPrefix, 
	                                       @Param("itemName") String itemName, 
	                                       Pageable pageable);

	// --- ADDED: Metrics Queries ---
	
	// Sum total value of all waste (actions starting with STOCK_WASTE_)
	@Query("SELECT COALESCE(SUM(a.totalValue), 0) FROM ActivityLogEntry a WHERE a.action LIKE 'STOCK_WASTE_%'")
	BigDecimal sumTotalWasteValue();

	// Count total waste entries
	@Query("SELECT COUNT(a) FROM ActivityLogEntry a WHERE a.action LIKE 'STOCK_WASTE_%'")
	long countTotalWasteEntries();

	// Sum value for specific waste type (e.g., STOCK_WASTE_EXPIRED)
	@Query("SELECT COALESCE(SUM(a.totalValue), 0) FROM ActivityLogEntry a WHERE a.action = :action")
	BigDecimal sumValueByAction(@Param("action") String action);
}