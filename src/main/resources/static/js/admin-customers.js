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
			const address1 = dataset.addressLine1 || '';
			const address2 = dataset.addressLine2 || '';
			const status = dataset.status || 'N/A';
			const createdAt = dataset.createdAt || 'N/A';
			const lastActivity = dataset.lastActivity || 'N/A';

			viewCustomerModal.querySelector('#viewCustomerModalLabel').textContent = 'Details for ' + name;
			viewCustomerModal.querySelector('#viewCustomerName').textContent = name;
			viewCustomerModal.querySelector('#viewCustomerUsername').textContent = username;
			viewCustomerModal.querySelector('#viewCustomerEmail').textContent = email;
			viewCustomerModal.querySelector('#viewCustomerPhone').textContent = phone;
			viewCustomerModal.querySelector('#viewCustomerAddress1').textContent = address1.trim();
			const address2Trimmed = address2.replace(/,\s*,/g, ',').replace(/^,\s*|,\s*$/g, '').trim();
			viewCustomerModal.querySelector('#viewCustomerAddress2').textContent = address2Trimmed.length > 0 ? address2Trimmed : 'No address details provided.';

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