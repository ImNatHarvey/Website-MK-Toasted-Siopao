document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-customers.js loaded");

	const mainElement = document.getElementById('admin-content-wrapper');
	if (!mainElement) {
		console.error("Main element not found in admin-customers.js!");
		return;
	}

	const viewCustomerModal = document.getElementById('viewCustomerModal');
	if (viewCustomerModal) {
		viewCustomerModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const customerRow = button ? button.closest('tr') : null;
			if (!customerRow) {
				console.warn("View Customer Modal opened without a valid row source.");
				return;
			}

			const dataset = customerRow.dataset;
			console.log("Populating View Customer Modal with data:", dataset);

			const name = dataset.name || 'N/A';
			const username = dataset.username || 'N/A';
			const email = dataset.email || 'N/A';
			const phone = dataset.phone || 'N/A';

			// --- MODIFIED: Build address string from individual fields ---
			const houseNo = dataset.houseNo && dataset.houseNo !== 'null' ? dataset.houseNo.trim() : '';
			const blockNo = dataset.blockNo && dataset.blockNo !== 'null' ? dataset.blockNo.trim() : '';
			const lotNo = dataset.lotNo && dataset.lotNo !== 'null' ? dataset.lotNo.trim() : '';
			const street = dataset.street && dataset.street !== 'null' ? dataset.street.trim() : '';
			const barangay = dataset.barangay && dataset.barangay !== 'null' ? dataset.barangay.trim() : '';
			const municipality = dataset.municipality && dataset.municipality !== 'null' ? dataset.municipality.trim() : '';
			const province = dataset.province && dataset.province !== 'null' ? dataset.province.trim() : '';

			let unitParts = [];
			if (houseNo) unitParts.push(`House No. ${houseNo}`);
			if (blockNo) unitParts.push(`Blk. No. ${blockNo}`);
			if (lotNo) unitParts.push(`Lot No. ${lotNo}`);

			let addressParts = [];
			const unitDetails = unitParts.join(', ');
			if (unitDetails) addressParts.push(unitDetails);

			if (street) addressParts.push(street);
			if (barangay) addressParts.push(barangay);
			if (municipality) addressParts.push(municipality);
			if (province) addressParts.push(province);

			const fullAddress = addressParts.join(', ');
			// --- END MODIFIED ---

			const status = dataset.status || 'N/A';
			const createdAt = dataset.createdAt || 'N/A';
			const lastActivity = dataset.lastActivity || 'N/A';

			viewCustomerModal.querySelector('#viewCustomerModalLabel').textContent = 'Details for ' + name;
			viewCustomerModal.querySelector('#viewCustomerName').textContent = name;
			viewCustomerModal.querySelector('#viewCustomerUsername').textContent = username;
			viewCustomerModal.querySelector('#viewCustomerEmail').textContent = email;
			viewCustomerModal.querySelector('#viewCustomerPhone').textContent = phone;

			// --- MODIFIED: Set the new single address span ---
			viewCustomerModal.querySelector('#viewCustomerAddress').textContent = fullAddress.length > 0 ? fullAddress : 'No address details provided.';
			// --- END MODIFIED ---

			const statusBadge = viewCustomerModal.querySelector('#viewCustomerStatusBadge');
			statusBadge.textContent = status;
			statusBadge.className = 'status-badge';
			if (status === 'ACTIVE') {
				statusBadge.classList.add('status-active');
			} else {
				statusBadge.classList.add('status-cancelled');
			}

			viewCustomerModal.querySelector('#viewCustomerCreatedAt').textContent = createdAt;
			viewCustomerModal.querySelector('#viewCustomerLastActivity').textContent = lastActivity;
		});
	}

	initializeModalForm({
		modalId: 'editCustomerModal',
		formId: 'editCustomerForm',
		validationAttribute: 'data-show-edit-customer-modal',
		wrapperId: 'admin-content-wrapper',
		editTriggerClass: 'edit-customer-btn',
		modalTitleSelector: '#editCustomerModalLabel',
		onShow: function(form, dataset, isEdit, isValidationReopen) {
			if (isEdit && dataset && !isValidationReopen) {
				const titleElement = document.getElementById('editCustomerModalLabel');
				if (titleElement) {
					titleElement.textContent = 'Edit Customer: ' + (dataset.firstName || '') + ' ' + (dataset.lastName || '');
				}
			}
		}
	});

	initializeModalForm({
		modalId: 'addCustomerModal',
		formId: 'addCustomerForm',
		validationAttribute: 'data-show-add-customer-modal',
		wrapperId: 'admin-content-wrapper'
	});
});