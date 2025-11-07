package com.toastedsiopao.repository;

import com.toastedsiopao.model.ActivityLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntry, Long> {

	// Find logs ordered by timestamp descending (newest first)
	Page<ActivityLogEntry> findAllByOrderByTimestampDesc(Pageable pageable);

	// You could add methods to find by username or date range later if needed
	// List<ActivityLogEntry> findByUsernameOrderByTimestampDesc(String username);
	// List<ActivityLogEntry>
	// findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime
	// end);
}