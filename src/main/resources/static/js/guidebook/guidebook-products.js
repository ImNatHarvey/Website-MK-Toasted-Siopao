(function() {
	window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

	const productGuide = {
		id: 'products',
		sortOrder: 4, // 4th Page
		path: '/admin/products',
		title: 'Product Management',
		content: `
            <div class="guide-scroll-area">
                <h5 class="text-primary mb-3">Managing Your Menu & Production</h5>
                <p>This module links your Inventory to your Sales. It handles the creation, pricing, and stock management of the finished goods (Siopao, Drinks, etc.) that customers order.</p>
                
                <hr>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-sliders me-2 text-info"></i>1. Top Controls</h6>
                    <p class="small text-muted mb-2">Use the buttons at the top right to manage the system:</p>
                    <ul class="small mb-0">
                        <li><span class="badge bg-primary"><i class="fa-solid fa-plus"></i> Add Product</span> Create a new menu item and define its recipe.</li>
                        <li><span class="badge bg-transparent text-primary border border-primary"><i class="fa-solid fa-tags"></i> Manage Categories</span> Organize items (e.g., "Special", "Drinks") for the menu.</li>
                        <li><span class="badge bg-success"><i class="fa-solid fa-boxes-stacked"></i> Manage Stock</span> The central hub for adding cooked products to your available stock.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-id-card me-2 text-warning"></i>2. Product Cards & Actions</h6>
                    <p class="small text-muted mb-2">Each card represents a menu item. Use the buttons at the bottom of the card:</p>
                    <ul class="small mb-0">
                        <li class="mb-1">
                            <span class="badge bg-info text-light">View</span> 
                            <strong>Details:</strong> See full info, ingredients list, and thresholds.
                        </li>
                        <li class="mb-1">
                            <span class="badge bg-warning text-dark">Edit</span> 
                            <strong>Modify:</strong> Change price, image, or description. <em class="text-danger">Note: Recipes cannot be changed once created (see below).</em>
                        </li>
                        <li>
                            <span class="badge bg-danger">Delete</span> / <span class="badge bg-success">Activate</span>
                            <strong>Status:</strong> If a product has sales history, you cannot Delete it; you can only <strong>Deactivate</strong> it to hide it from the menu.
                        </li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-utensils me-2 text-secondary"></i>3. The Recipe System</h6>
                    <div class="alert alert-info small p-2 mb-2">
                        <i class="fa-solid fa-lock me-1"></i> <strong>Recipe Lock:</strong> 
                        Once a product is saved, its ingredient list is <strong>LOCKED</strong>. This prevents historical data corruption (e.g., calculating costs for past orders). To change a recipe, you must create a new product version.
                    </div>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-industry me-2 text-success"></i>4. Production (Adding Stock)</h6>
                    <p class="small text-muted mb-2">When you click <strong>Manage Stock</strong>, you will see a table of all products.</p>
                    <ul class="small mb-0">
                        <li><strong>Automatic Deduction:</strong> When you select <em>"Production"</em> and add quantity (e.g., +50 Siopao), the system automatically <strong>deducts</strong> the necessary flour, meat, and eggs from your <em>Inventory</em> based on the recipe.</li>
                        <li><strong>Max Button:</strong> Click the <span class="badge bg-info text-light">Max</span> button on a row to instantly calculate how many units you can cook based on your currently available ingredients.</li>
                        <li><strong>Validation:</strong> The system will block production if you lack the required raw ingredients.</li>
                    </ul>
                </div>
                
                <div class="alert alert-info small">
                    <i class="fa-solid fa-lightbulb me-1"></i> <strong>Tip:</strong> 
                    If you need to correct a product's count without affecting raw inventory (e.g., a counting error), use the <strong>Manual Adjust</strong> reason in the Manage Stock modal.
                </div>
            </div>
        `
	};

	window.MK_GUIDEBOOK_REGISTRY.push(productGuide);
})();