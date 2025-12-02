(function() {
	window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

	const activityLogGuide = {
		id: 'activity-log',
		sortOrder: 9, // 9th Page
		path: '/admin/activity-log',
		title: 'Activity Log',
		content: `
            <div class="guide-scroll-area">
                <h5 class="text-primary mb-3">Audit Trail & Security</h5>
                <p>This page maintains a chronological record of all sensitive actions performed within the admin portal. It is essential for accountability and security monitoring.</p>
                
                <hr>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-table-list me-2 text-info"></i>1. Reading the Log</h6>
                    <p class="small text-muted mb-2">Each row represents a single action:</p>
                    <ul class="small mb-0">
                        <li><strong>Timestamp:</strong> Exact date and time the action occurred.</li>
                        <li><strong>User:</strong> The username of the admin who performed the action.</li>
                        <li><strong>Action:</strong> The type of operation (e.g., <em>ADD_PRODUCT</em>, <em>EDIT_ORDER</em>, <em>DELETE_USER</em>).</li>
                        <li><strong>Details:</strong> Specifics about what changed (e.g., "Updated stock for Siopao: +50").</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-magnifying-glass me-2 text-warning"></i>2. Investigation Tools</h6>
                    <p class="small text-muted mb-2">Use the filters at the top to find specific events:</p>
                    <ul class="small mb-0">
                        <li><strong>Keyword Search:</strong> Filters by Username, Action name, or the content of the Details message.</li>
                        <li><strong>Date Range:</strong> Define a specific <em>From</em> and <em>To</em> window to narrow down the timeline.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-file-export me-2 text-success"></i>3. Archiving Records</h6>
                    <p class="small text-muted mb-2">For long-term storage or auditing purposes:</p>
                     <ul class="list-group list-group-flush small">
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                            <button class="btn btn-sm btn-action-success me-3 pe-none" style="width: 80px; opacity: 1;"><i class="fa-solid fa-file-pdf"></i> Export</button>
                            <div>Generates a <strong>PDF</strong> report of the currently filtered view.</div>
                        </li>
                    </ul>
                </div>
                
                <div class="alert alert-info small">
                    <i class="fa-solid fa-lightbulb me-1"></i> <strong>Tip:</strong> 
                    If inventory numbers don't match up, use this log to check if a <strong>Manual Adjust</strong> or <strong>Waste</strong> entry was recorded by a staff member recently.
                </div>
            </div>
        `
	};

	window.MK_GUIDEBOOK_REGISTRY.push(activityLogGuide);
})();