(function() {
	window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

	const transactionGuide = {
		id: 'transactions',
		sortOrder: 5, // 5th Page
		path: '/admin/transactions',
		title: 'Transaction History',
		content: `
            <div class="guide-scroll-area">
                <h5 class="text-primary mb-3">Financial Records & Auditing</h5>
                <p>This page is your permanent record of all completed and verified sales. Unlike <em>Order Management</em>, which focuses on workflow, this page is optimized for accounting and historical analysis.</p>
                
                <hr>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-chart-line me-2 text-info"></i>1. Financial Snapshot</h6>
                    <p class="small text-muted mb-2">The cards at the top summarize your performance based on the current search filters (or all-time if no filters are set):</p>
                    <ul class="small mb-0">
                        <li><strong>Total Revenue:</strong> The sum of all <em>completed</em> payments.</li>
                        <li><strong>Total Transactions:</strong> The count of successful orders.</li>
                        <li><strong>Average Order Value:</strong> Useful for understanding customer spending habits.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-filter me-2 text-warning"></i>2. Filtering Data</h6>
                    <p class="small text-muted mb-2">Narrow down records for specific reports:</p>
                    <ul class="small mb-0">
                        <li><strong>Date Range:</strong> Select a <em>From</em> and <em>To</em> date to see sales for a specific period (e.g., "Last Month").</li>
                        <li><strong>Search:</strong> Find specific transactions by <strong>Order ID</strong> or <strong>Customer Name</strong>.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-file-export me-2 text-success"></i>3. Exporting Reports</h6>
                    <p class="small text-muted mb-2">Need to share data with an accountant? Click the green <strong>Export</strong> button:</p>
                    <div class="d-flex gap-2 small">
                        <div class="border rounded p-2 flex-fill text-center bg-light">
                            <i class="fa-solid fa-file-excel text-success mb-1 d-block"></i>
                            <strong>Excel</strong><br>Best for spreadsheets & calculation.
                        </div>
                        <div class="border rounded p-2 flex-fill text-center bg-light">
                            <i class="fa-solid fa-file-pdf text-danger mb-1 d-block"></i>
                            <strong>PDF</strong><br>Best for printing & formal submission.
                        </div>
                    </div>
                </div>
                
                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-flag me-2 text-danger"></i>4. Monitoring Issues</h6>
                    <p class="small text-muted mb-2">Even completed transactions might have customer reports. Check the <strong>Actions</strong> column:</p>
                    <ul class="list-group list-group-flush small">
                         <li class="list-group-item bg-transparent px-0 py-1">
                            <button class="btn btn-sm btn-custom-outline me-2 pe-none" style="opacity: 1; border-radius: 4px;"><i class="fa-solid fa-flag"></i></button>
                            <strong>Issue Flag:</strong> Click this button to view any reported problems. If a red badge appears on it, there is an <em>Open</em> issue requiring attention.
                        </li>
                        <li class="list-group-item bg-transparent px-0 py-1">
                            <strong>Resolution:</strong> Inside the modal, you can view details, see attached photos, and mark the issue as <em>Resolved</em>.
                        </li>
                    </ul>
                </div>
                
                <div class="alert alert-info small">
                    <i class="fa-solid fa-lightbulb me-1"></i> <strong>Tip:</strong> 
                    Click <strong>Clear Search</strong> to reset all filters and view your lifetime revenue totals again.
                </div>
            </div>
        `
	};

	window.MK_GUIDEBOOK_REGISTRY.push(transactionGuide);
})();