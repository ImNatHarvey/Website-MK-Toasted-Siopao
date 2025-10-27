/**
 * Main script file for admin portal features.
 */
document.addEventListener('DOMContentLoaded', function() {

	// Find the main element that holds our data attributes from Thymeleaf
	const mainElement = document.querySelector('main');

	if (!mainElement) {
		return;
	}

	// --- Logic to re-open modal on validation error ---

	// Check for "Manage Categories" modal trigger
	const showCategoryModal = mainElement.dataset.showCategoryModal;
	if (showCategoryModal === 'true') {
		const categoryModalElement = document.getElementById('manageCategoriesModal');
		if (categoryModalElement) {
			const categoryModal = new bootstrap.Modal(categoryModalElement);
			categoryModal.show();
		}
	}

	// Check for "Add Product" modal trigger
	const showAddProductModal = mainElement.dataset.showAddProductModal;
	if (showAddProductModal === 'true') {
		const productModalElement = document.getElementById('addProductModal');
		if (productModalElement) {
			const productModal = new bootstrap.Modal(productModalElement);
			productModal.show();
		}
	}

	// --- UPDATED: Check for "Manage Admins" modal trigger ---
	const showManageAdminsModal = mainElement.dataset.showManageAdminsModal;
	if (showManageAdminsModal === 'true') {
		const adminModalElement = document.getElementById('manageAdminsModal');
		if (adminModalElement) {
			const adminModal = new bootstrap.Modal(adminModalElement);
			adminModal.show();
		}
	}

	// Check for "Add Customer" modal trigger
	const showAddCustomerModal = mainElement.dataset.showAddCustomerModal;
	if (showAddCustomerModal === 'true') {
		const customerModalElement = document.getElementById('addCustomerModal');
		if (customerModalElement) {
			const customerModal = new bootstrap.Modal(customerModalElement);
			customerModal.show();
		}
	}

	// --- NEW: Logic for "View Customer" Modal ---
	const viewCustomerModal = document.getElementById('viewCustomerModal');
	if (viewCustomerModal) {
		viewCustomerModal.addEventListener('show.bs.modal', function(event) {
			// Button that triggered the modal
			const button = event.relatedTarget;

			// Find the closest parent <tr> to get all data attributes
			const customerRow = button.closest('tr');

			// Extract data from data-* attributes
			const name = customerRow.dataset.name;
			const username = customerRow.dataset.username;
			const phone = customerRow.dataset.phone;
			const address1 = customerRow.dataset.addressLine1;
			const address2 = customerRow.dataset.addressLine2;

			// Find elements inside the modal
			const modalTitle = viewCustomerModal.querySelector('#viewCustomerModalLabel');
			const modalName = viewCustomerModal.querySelector('#viewCustomerName');
			const modalUsername = viewCustomerModal.querySelector('#viewCustomerUsername');
			const modalPhone = viewCustomerModal.querySelector('#viewCustomerPhone');
			const modalAddress1 = viewCustomerModal.querySelector('#viewCustomerAddress1');
			const modalAddress2 = viewCustomerModal.querySelector('#viewCustomerAddress2');

			// Update the modal's content
			modalTitle.textContent = 'Details for ' + name;
			modalName.textContent = name;
			modalUsername.textContent = username;
			modalPhone.textContent = phone;

			// Only show address lines if they have content
			modalAddress1.textContent = (address1.trim().length > 0) ? address1 : '';
			modalAddress2.textContent = (address2.trim().length > 1) ? address2 : 'No address provided.'; // (', ' is 2 chars)
		});
	}

});