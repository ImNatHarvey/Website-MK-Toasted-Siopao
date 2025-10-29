/**
 * Main script file - Handles general functionality like modal reopening.
 * Page-specific modal logic is now in separate files (admin-products.js, etc.)
 */
document.addEventListener('DOMContentLoaded', function() {

	const mainElement = document.querySelector('main');
	if (!mainElement) {
		console.error("Main element not found!");
		return;
	}

	// --- REVISED Logic to re-open modal on validation error ---
	// We check slightly after DOMContentLoaded, giving Thymeleaf/Browser time to apply attributes
	setTimeout(() => {
		console.log("Checking for modals to reopen..."); // Debug log
		const modalsToReopen = {
			// Product Modals
			'manageCategoriesModal': mainElement.dataset.showCategoryModal, // Used by products page
			'addProductModal': mainElement.dataset.showAddProductModal,
			'editProductModal': mainElement.dataset.showEditProductModal,
			'manageStockModal': mainElement.dataset.showManageStockModal, // Used by products page too
			'viewProductModal': mainElement.dataset.showViewProductModal,
			// Inventory Modals (Note: Some IDs overlap with Products, ensure unique if needed)
			'addItemModal': mainElement.dataset.showAddItemModal,
			'manageCategoriesModalInv': mainElement.dataset.showManageCategoriesModal, // Renamed key for inventory context if needed
			'manageUnitsModal': mainElement.dataset.showManageUnitsModal,
			'manageStockModalInv': mainElement.dataset.showManageStockModal, // Renamed key for inventory context if needed
			// User Modals
			'manageAdminsModal': mainElement.dataset.showManageAdminsModal,
			'addCustomerModal': mainElement.dataset.showAddCustomerModal,
			'editCustomerModal': mainElement.dataset.showEditCustomerModal,
			'editAdminModal': mainElement.dataset.showEditAdminModal
		};

		console.log("Modal flags found:", mainElement.dataset); // Debug log of all data attributes

		for (const modalId in modalsToReopen) {
			const shouldShow = modalsToReopen[modalId];
			console.log(`Checking modalId: ${modalId}, shouldShow: ${shouldShow}`); // Debug log

			// Check specifically for the string 'true'
			if (shouldShow === 'true') {
				// Use the original modal ID from the key for finding the element
				const actualModalId = modalId.replace('Inv', ''); // Remove Inv suffix if present
				const modalElement = document.getElementById(actualModalId);

				if (modalElement) {
					console.log(`Attempting to show modal: #${actualModalId}`); // Debug log
					if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
						try {
							// Get existing instance or create a new one
							const modalInstance = bootstrap.Modal.getOrCreateInstance(modalElement);
							modalInstance.show();
							console.log(`Successfully called show() for #${actualModalId}`); // Debug log
							// OPTIONAL: Remove the data attribute after showing to prevent re-showing on back navigation?
							// mainElement.removeAttribute(`data-${modalId.replace(/([A-Z])/g, '-$1').toLowerCase()}`);
						} catch (e) {
							console.error(`Error showing modal ${actualModalId}:`, e);
						}
					} else {
						console.error('Bootstrap Modal component not found.');
					}
				} else {
					console.warn(`Modal element #${actualModalId} not found.`); // Debug log
				}
			}
		}
	}, 100); // Execute after a small delay (100 milliseconds)

	// --- REMOVED ALL PAGE-SPECIFIC MODAL EVENT LISTENERS ---
	// (e.g., viewCustomerModal, editCustomerModal, addItemModal, editProductModal, viewProductModal logic)
	// --- REMOVED RECIPE INGREDIENT MANAGEMENT FUNCTIONS ---
	// (addIngredientRow, removeIngredientRow, renumberIngredientRows)
	// --- REMOVED MAX BUTTON LOGIC ---

}); // End DOMContentLoaded