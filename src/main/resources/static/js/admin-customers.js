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

	const editCustomerModal = document.getElementById('editCustomerModal');
	if (editCustomerModal) {
		const form = editCustomerModal.querySelector('#editCustomerForm');
		const modalTitle = editCustomerModal.querySelector('#editCustomerModalLabel');

		editCustomerModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('edit-customer-btn')) {
				console.warn("Edit Customer modal opened without edit button source.");
				if (form) form.reset();
				return;
			}
			const dataset = button.dataset;
			console.log("Populating Edit Customer Modal with data:", dataset); // Debug

			modalTitle.textContent = 'Edit Customer: ' + (dataset.firstName || '') + ' ' + (dataset.lastName || '');
			if (!form) { console.error("Edit customer form not found!"); return; }

			form.querySelector('#id').value = dataset.id || '';
			form.querySelector('#firstName').value = dataset.firstName || '';
			form.querySelector('#lastName').value = dataset.lastName || '';
			form.querySelector('#username').value = dataset.username || '';
			form.querySelector('#email').value = dataset.email || '';
			form.querySelector('#phone').value = dataset.phone || '';
			form.querySelector('#houseNo').value = dataset.houseNo || '';
			form.querySelector('#lotNo').value = dataset.lotNo || '';
			form.querySelector('#blockNo').value = dataset.blockNo || '';
			form.querySelector('#street').value = dataset.street || '';
			form.querySelector('#barangay').value = dataset.barangay || '';
			form.querySelector('#municipality').value = dataset.municipality || '';
			form.querySelector('#province').value = dataset.province || '';
			form.querySelector('#status').value = dataset.status || 'ACTIVE';

			if (mainElement.dataset.showEditCustomerModal !== 'true') {
				console.log("Clearing validation highlights on modal show (not validation reopen).");
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger');
				if (errorAlert && errorAlert.getAttribute('th:if') === null) {
					errorAlert.remove();
				}
			} else {
				console.log("Modal is being reopened due to validation, NOT clearing highlights.");
			}
		});

		editCustomerModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditCustomerModal !== 'true') {
				console.log("Clearing Edit Customer modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form ? form.querySelector('.alert.alert-danger') : null;
				if (errorAlert && errorAlert.getAttribute('th:if') === null) {
					errorAlert.remove();
				}
			} else {
				console.log("Resetting showEditCustomerModal flag on hide.")
				mainElement.removeAttribute('data-show-edit-customer-modal');
			}
		});
	}

	const addCustomerModal = document.getElementById('addCustomerModal');
	if (addCustomerModal) {
		const form = addCustomerModal.querySelector('#addCustomerForm');

		addCustomerModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showAddCustomerModal !== 'true') {
				console.log("Clearing Add Customer modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showAddCustomerModal flag on hide.")
				mainElement.removeAttribute('data-show-add-customer-modal');
			}
		});
	}
});