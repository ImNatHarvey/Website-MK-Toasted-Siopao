document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-guidebook.js loaded.");

	const guidebookModal = document.getElementById('guidebookModal');
	const guidebookTitle = document.getElementById('guidebookTitle');
	const guidebookContent = document.getElementById('guidebookContent');

	// Define Guide Content for each page
	const guides = {
		'/admin/dashboard': {
			title: 'Admin Guide',
			content: `
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
                        <li><strong>Pending (Verification):</strong> GCash orders waiting for you to verify the payment receipt.</li>
                        <li><strong>Pending (COD):</strong> Cash on Delivery orders waiting for approval.</li>
                        <li><strong>Processing:</strong> Orders currently being prepared in the kitchen.</li>
                        <li><strong>Out for Delivery:</strong> Orders picked up by the rider.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6><i class="fa-solid fa-boxes-stacked me-2 text-success"></i>Inventory & Products</h6>
                    <p class="small text-muted">Quick alerts for your stock levels.</p>
                    <ul class="small">
                        <li><strong>Low/Critical Stock:</strong> Items that are running low and need restocking soon.</li>
                        <li><strong>Out of Stock:</strong> Items currently at 0 quantity. These won't be usable in recipes.</li>
                    </ul>
                </div>

                <div class="alert alert-info small">
                    <i class="fa-solid fa-lightbulb me-1"></i> <strong>Tip:</strong> Use the 
                    <span class="badge bg-success"><i class="fa-solid fa-file-pdf"></i> Export Dashboard PDF</span> 
                    button at the top right to save a snapshot of this report.
                </div>
            `
		},
		'/admin/settings': {
			title: 'Site Management Guide',
			content: `
                <h5 class="text-primary mb-3">Managing Your Website Content</h5>
                <p>This page allows you to customize the public-facing look and feel of your website without needing to touch code.</p>
                
                <hr>

                <div class="mb-4">
                    <h6><i class="fa-solid fa-globe me-2 text-info"></i>General & Branding</h6>
                    <p class="small text-muted">Update your brand identity.</p>
                    <ul class="small">
                        <li><strong>Website Name:</strong> Appears in the browser tab and top navigation bar.</li>
                        <li><strong>Logo & Favicon:</strong> Upload your store logo (navbar) and favicon (browser tab icon).</li>
                        <li><strong>Footer Text:</strong> The copyright or contact line at the very bottom of every page.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6><i class="fa-solid fa-credit-card me-2 text-warning"></i>Payment Settings</h6>
                    <p class="small text-muted">Configure details shown to customers during GCash checkout.</p>
                    <ul class="small">
                        <li><strong>GCash QR Code:</strong> Upload the QR image customers will scan.</li>
                        <li><strong>Account Name/Number:</strong> Displayed alongside the QR code for manual transfers.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6><i class="fa-solid fa-images me-2 text-success"></i>Homepage Content</h6>
                    <p class="small text-muted">Control the marketing content on the landing page.</p>
                    <ul class="small">
                        <li><strong>Carousel:</strong> Upload up to 3 banner images that slide automatically.</li>
                        <li><strong>Featured Products:</strong> Customize the 4 highlight cards (e.g., Best Sellers, Combos) with images and text.</li>
                        <li><strong>Promo & "Why Us":</strong> Update the text and images for your special offers and value proposition sections.</li>
                    </ul>
                </div>
                
                <div class="alert alert-warning small">
                    <i class="fa-solid fa-floppy-disk me-1"></i> <strong>Note:</strong> 
                    Don't forget to click the <strong>Save All Changes</strong> button at the bottom of the page to apply your updates!
                </div>
            `
		},
		'default': {
			title: 'Admin Guide',
			content: `
                <div class="text-center py-4">
                    <i class="fa-solid fa-book text-muted fa-3x mb-3"></i>
                    <h5>Guide Coming Soon</h5>
                    <p class="text-muted">The tutorial for this specific page is currently being written.</p>
                </div>
            `
		}
	};

	// Function to determine current page path
	function getCurrentPath() {
		const path = window.location.pathname;
		// Simple path matching
		if (path.startsWith('/admin/dashboard')) return '/admin/dashboard';
		if (path.startsWith('/admin/settings')) return '/admin/settings';

		return 'default';
	}

	// Load content when modal is opened
	if (guidebookModal) {
		guidebookModal.addEventListener('show.bs.modal', function() {
			const currentPath = getCurrentPath();
			const data = guides[currentPath] || guides['default'];

			if (guidebookTitle) guidebookTitle.textContent = data.title;
			if (guidebookContent) guidebookContent.innerHTML = data.content;
		});
	}
});