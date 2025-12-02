(function() {
	window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

	const dashboardGuide = {
		id: 'dashboard',
		sortOrder: 1, // 1st Page
		path: '/admin/dashboard',
		title: 'Dashboard Overview',
		content: `
            <div class="guide-scroll-area">
                <h5 class="text-primary mb-3">Welcome to the Admin Dashboard!</h5>
                <p>This page serves as the command center for your business, providing a real-time overview of performance and activities.</p>
                
                <hr>

                <div class="mb-4">
                    <h6><i class="fa-solid fa-chart-line me-2 text-info"></i>Financial Summary</h6>
                    <p class="small text-muted">Located at the top, this section tracks your daily, weekly, and monthly financial health.</p>
                    <ul class="small">
                        <li><strong>Sales:</strong> Total revenue from delivered/paid orders.</li>
                        <li><strong>Est. COGS (Cost of Goods Sold):</strong> The calculated cost of ingredients used for sold items.</li>
                        <li><strong>Gross Profit:</strong> Sales minus COGS. This is your estimated earning before other expenses.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6><i class="fa-solid fa-box me-2 text-warning"></i>Order Summary</h6>
                    <p class="small text-muted">A breakdown of orders by their current status.</p>
                    <ul class="small">
                        <li><strong>Pending (Verification):</strong> GCash orders waiting for payment verification.</li>
                        <li><strong>Pending (COD):</strong> Cash on Delivery orders waiting for approval.</li>
                        <li><strong>Processing:</strong> Orders currently being prepared in the kitchen.</li>
                        <li><strong>Out for Delivery:</strong> Orders picked up by the rider.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6><i class="fa-solid fa-boxes-stacked me-2 text-success"></i>Inventory & Products</h6>
                    <p class="small text-muted">Quick alerts for your stock levels.</p>
                    <ul class="small">
                        <li><strong>Low/Critical Stock:</strong> Items running low that need restocking.</li>
                        <li><strong>Out of Stock:</strong> Items at 0 quantity (unusable in recipes).</li>
                    </ul>
                </div>

                <div class="alert alert-info small">
                    <i class="fa-solid fa-lightbulb me-1"></i> <strong>Tip:</strong> Use the 
                    <span class="badge bg-success"><i class="fa-solid fa-file-pdf"></i> Export Dashboard PDF</span> 
                    button at the top right to save a snapshot of this report.
                </div>
            </div>
        `
	};

	window.MK_GUIDEBOOK_REGISTRY.push(dashboardGuide);
})();