(function() {
	window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

	const inventoryGuide = {
		id: 'inventory',
		sortOrder: 3, // 3rd Page
		path: '/admin/inventory',
		title: 'Inventory Management',
		content: `
            <div class="guide-scroll-area">
                <h5 class="text-primary mb-3">Controlling Your Stock</h5>
                <p>This module manages your raw ingredients and supplies. It is divided into two main sections accessible via tabs:</p>
                
                <hr>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-table-columns me-2 text-info"></i>1. The Two Views</h6>
                    <ul class="list-group list-group-flush small">
                        <li class="list-group-item bg-transparent px-0 py-1">
                            <strong><i class="fa-solid fa-list me-1"></i> Inventory List:</strong> 
                            The main view. Shows current stock levels, costs, and status for all active items.
                        </li>
                        <li class="list-group-item bg-transparent px-0 py-1">
                            <strong><i class="fa-solid fa-trash-can me-1 text-danger"></i> Waste & Spoilage:</strong> 
                            A dedicated log history of all items deducted due to expiration, damage, or waste.
                        </li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-chart-pie me-2 text-secondary"></i>2. Dashboard Metrics</h6>
                    <p class="small text-muted mb-2">The top cards provide a health check of your inventory:</p>
                    <ul class="small mb-0">
                        <li><strong>Total Inventory Value:</strong> The sum monetary value of all active items (<em>Current Stock × Cost Per Unit</em>).</li>
                        <li><strong>Items Low/Critical:</strong> A count of items that have fallen below your safety thresholds.</li>
                        <li><strong>Items Out of Stock:</strong> Items with 0 quantity. These prevent production of linked products.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-tags me-2 text-warning"></i>3. Status & Stock Levels</h6>
                    <p class="small text-muted mb-2">Badges indicate the health of each item:</p>
                    <div class="row g-2 small">
                        <div class="col-6 d-flex align-items-center">
                             <span class="badge bg-success me-2" style="width: 80px;">NORMAL</span>
                            <div>Stock is sufficient.</div>
                        </div>
                        <div class="col-6 d-flex align-items-center">
                             <span class="badge bg-warning text-dark me-2" style="width: 80px;">LOW</span>
                            <div>Below low threshold. Re-order soon.</div>
                        </div>
                        <div class="col-6 d-flex align-items-center">
                             <span class="badge bg-danger me-2" style="width: 80px;">CRITICAL</span>
                            <div>Below critical threshold. Action needed.</div>
                        </div>
                        <div class="col-6 d-flex align-items-center">
                             <span class="badge bg-dark me-2" style="width: 80px;">NO STOCK</span>
                            <div>Empty (0 units).</div>
                        </div>
                         <div class="col-6 d-flex align-items-center">
                             <span class="badge bg-secondary me-2" style="width: 80px;">INACTIVE</span>
                            <div>Hidden/Archived item.</div>
                        </div>
                    </div>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-boxes-stacked me-2 text-success"></i>4. Managing Stock</h6>
                    <p class="small text-muted mb-2">Click the green <strong>Manage Stock</strong> button. The action changes based on the <em>Reason</em>:</p>
                    
                    <ul class="list-group list-group-flush small">
                         <li class="list-group-item d-flex align-items-start bg-transparent px-0 py-2">
                            <span class="badge bg-success me-2" style="width: 80px;">ADD</span>
                            <div><strong>Production:</strong> Adds to current stock. Use this when receiving new deliveries.</div>
                        </li>
                         <li class="list-group-item d-flex align-items-start bg-transparent px-0 py-2">
                            <span class="badge bg-warning text-dark me-2" style="width: 80px;">ADJUST</span>
                            <div><strong>Manual Adjust:</strong> Overwrites stock to a specific number. Use for audit corrections.</div>
                        </li>
                         <li class="list-group-item d-flex align-items-start bg-transparent px-0 py-2">
                            <span class="badge bg-danger me-2" style="width: 80px;">DEDUCT</span>
                            <div><strong>Waste/Expired:</strong> Removes stock and automatically logs it to the Waste tab.</div>
                        </li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-file-export me-2 text-primary"></i>5. Reports & Exporting</h6>
                    <p class="small text-muted mb-0">Switch to the <strong>Waste & Spoilage</strong> tab to generate reports. Use the filters to select a date range or reason (e.g., Expired), then click <strong>Export</strong> to download a PDF or Excel file for accounting.</p>
                </div>
                
                <div class="alert alert-info small">
                    <i class="fa-solid fa-lightbulb me-1"></i> <strong>Tip:</strong> 
                    Use the <strong>Max</strong> button on an item row to see how many product units you can make with that specific ingredient!
                </div>
            </div>
        `
	};

	window.MK_GUIDEBOOK_REGISTRY.push(inventoryGuide);
})();