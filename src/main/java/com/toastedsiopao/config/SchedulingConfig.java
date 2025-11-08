package com.toastedsiopao.config;

import com.toastedsiopao.service.CustomerService; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class SchedulingConfig {

	private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

	@Autowired
	private CustomerService customerService; 

	@Scheduled(cron = "0 0 3 * * ?")
	public void runDailyInactivityCheck() {
		log.info("--- [SCHEDULER] Starting daily inactivity check... ---");
		try {
			customerService.checkForInactiveCustomers();
		} catch (Exception e) {
			log.error("--- [SCHEDULER] Error during daily inactivity check: {} ---", e.getMessage(), e);
		}
		log.info("--- [SCHEDULER] Finished daily inactivity check. ---");
	}
}