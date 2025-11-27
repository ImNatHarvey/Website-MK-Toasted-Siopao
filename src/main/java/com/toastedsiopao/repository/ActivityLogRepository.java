package com.toastedsiopao.repository;

import com.toastedsiopao.model.ActivityLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntry, Long> {

	Page<ActivityLogEntry> findAllByOrderByTimestampDesc(Pageable pageable);

	// Efficiently fetch logs where action starts with a prefix (e.g., "STOCK_WASTE_")
	Page<ActivityLogEntry> findByActionStartingWithOrderByTimestampDesc(String actionPrefix, Pageable pageable);
}