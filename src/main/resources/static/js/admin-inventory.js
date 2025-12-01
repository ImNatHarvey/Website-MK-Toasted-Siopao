document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-inventory.js loaded");

	const mainElement = document.getElementById('admin-content-wrapper');

	if (!mainElement) {
		console.error("Main element not found in admin-inventory.js!");
		return;
	}

	// --- addItemModal (Complex) ---
	const addItemModal = document.getElementById('addItemModal');
	if (addItemModal) {
		const itemForm = addItemModal.querySelector('#itemForm');
		const modalTitle = addItemModal.querySelector('#addItemModalLabel');
		const itemIdInput = addItemModal.querySelector('#itemId');
		const itemNameInput = addItemModal.querySelector('#itemName');
		const itemCategorySelect = addItemModal.querySelector('#itemCategory');
		const itemUnitSelect = addItemModal.querySelector('#itemUnit');
		const itemStatusSelect = addItemModal.querySelector('#itemStatus');
		const itemStockInfoContainer = addItemModal.querySelector('#itemStockInfoContainer');
		const itemStockHiddenInput = addItemModal.querySelector('#itemCurrentStockHidden');
		const itemLowThresholdInput = addItemModal.querySelector('#itemLowThreshold');
		const itemCriticalThresholdInput = addItemModal.querySelector('#itemCriticalThreshold');
		const itemCostInput = addItemModal.querySelector('#itemCost');

		addItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isEdit = button && button.classList.contains('edit-item-btn');

			const isValidationReopen = mainElement.dataset.showAddItemModal === 'true';
			const dataset = isEdit ? button.dataset : {};

			if (!isValidationReopen) {
				if (itemForm) {
					itemForm.reset();
					itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
					itemForm.querySelectorAll('.invalid-feedback').forEach(el => {
						if (el.getAttribute('th:if') === null) {
							el.textContent = '';
							el.classList.remove('d-block');
						}
					});
				}
				itemIdInput.value = '';
				itemStockInfoContainer.style.display = 'none';

				itemNameInput.value = '';
				itemCategorySelect.value = '';
				itemUnitSelect.value = '';
				itemCostInput.value = '';
				itemLowThresholdInput.value = '';
				itemCriticalThresholdInput.value = '';
				if (itemStatusSelect) itemStatusSelect.value = 'ACTIVE';
			}

			let isExistingItem = isEdit || (isValidationReopen && itemIdInput.value);

			if (itemStatusSelect) itemStatusSelect.disabled = false;

			if (isExistingItem) {
				if (isEdit && !isValidationReopen) {
					console.log("Fresh Edit: Populating fields from button dataset.");
					itemIdInput.value = dataset.id || '';
					itemNameInput.value = dataset.name || '';
					itemCategorySelect.value = dataset.categoryId || '';
					itemUnitSelect.value = dataset.unitId || '';
					itemStatusSelect.value = dataset.itemStatus || 'ACTIVE';
					if (itemStockHiddenInput) itemStockHiddenInput.value = dataset.currentStock || '0.00';
					itemLowThresholdInput.value = parseFloat(dataset.lowThreshold || '0').toFixed(0);
					itemCriticalThresholdInput.value = parseFloat(dataset.criticalThreshold || '0').toFixed(0);
					itemCostInput.value = dataset.cost || '';
				}
				modalTitle.textContent = 'Edit Inventory Item: ' + (itemNameInput.value || 'N/A');
				itemStockInfoContainer.style.display = 'block';

			} else {
				modalTitle.textContent = 'Add New Inventory Item';
				itemStockInfoContainer.style.display = 'none';

				if (!isValidationReopen) {
					if (itemStatusSelect) itemStatusSelect.value = 'ACTIVE';
				}
			}
			initThresholdSliders(addItemModal);
		});

		addItemModal.addEventListener('hidden.bs.modal', function() {
			const isValidationReopen = mainElement.dataset.showAddItemModal === 'true';

			if (isValidationReopen) {
				mainElement.removeAttribute('data-show-add-item-modal');
			}
			if (itemStatusSelect) itemStatusSelect.disabled = false;
		});
	}

	// --- manageCategoriesModal (Complex) - Left as-is ---
	const manageCategoriesModal = document.getElementById('manageCategoriesModal');
	if (manageCategoriesModal) {
		const form = manageCategoriesModal.querySelector('#addInvCategoryForm');

		manageCategoriesModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageCategoriesModal !== 'true') {
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				mainElement.removeAttribute('data-show-manage-categories-modal');
			}
		});
	}

	// --- editInvCategoryModal (Simple) - REFACTORED ---
	initializeModalForm({
		modalId: 'editInvCategoryModal',
		formId: 'editInvCategoryForm',
		validationAttribute: 'data-show-edit-inv-category-modal',
		wrapperId: 'admin-content-wrapper',
		editTriggerClass: 'edit-inv-category-btn'
	});

	// --- manageUnitsModal (Complex) - Left as-is ---
	const manageUnitsModal = document.getElementById('manageUnitsModal');
	if (manageUnitsModal) {
		const form = manageUnitsModal.querySelector('#addUnitForm');

		manageUnitsModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageUnitsModal !== 'true') {
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const globalError = form ? form.querySelector('.invalid-feedback.d-block') : null;
				if (globalError) globalError.textContent = '';
			} else {
				mainElement.removeAttribute('data-show-manage-units-modal');
			}
		});
	}

	// --- editUnitModal (Simple) - REFACTORED ---
	initializeModalForm({
		modalId: 'editUnitModal',
		formId: 'editUnitForm',
		validationAttribute: 'data-show-edit-unit-modal',
		wrapperId: 'admin-content-wrapper',
		editTriggerClass: 'edit-unit-btn'
	});

	// --- manageStockModal (Complex) - UPDATE LOGIC FOR BUTTON TOGGLE ---
	const manageStockModal = document.getElementById('manageStockModal');
	if (manageStockModal) {

		// Function to update button visibility based on reason
		const updateStockActionButtons = (row, reason) => {
			const addBtn = row.querySelector('.btn-add');
			const setBtn = row.querySelector('.btn-set');
			const deductBtn = row.querySelector('.btn-deduct');

			if (!addBtn || !setBtn || !deductBtn) return;

			// Hide all first
			addBtn.style.display = 'none';
			addBtn.disabled = true;

			setBtn.style.display = 'none';
			setBtn.disabled = true;

			deductBtn.style.display = 'none';
			deductBtn.disabled = true;

			if (reason === 'Production') {
				addBtn.style.display = 'block';
				addBtn.disabled = false;
			} else if (reason === 'Manual') {
				setBtn.style.display = 'block';
				setBtn.disabled = false;
			} else if (['Expired', 'Damaged', 'Waste'].includes(reason)) {
				deductBtn.style.display = 'block';
				deductBtn.disabled = false;
			}
		};

		manageStockModal.addEventListener('change', function(e) {
			if (e.target && e.target.classList.contains('reason-category-select')) {
				const reason = e.target.value;
				const row = e.target.closest('tr');
				updateStockActionButtons(row, reason);
			}
		});

		// Initialize buttons on modal show (reset to Production/Add)
		manageStockModal.addEventListener('show.bs.modal', function() {
			const rows = manageStockModal.querySelectorAll('tbody tr');
			rows.forEach(row => {
				const select = row.querySelector('.reason-category-select');
				if (select) {
					select.value = 'Production'; // Reset to default
					updateStockActionButtons(row, 'Production');
				}
				// Clear visible inputs only
				const qtyInput = row.querySelector('.stock-qty-input');
				const noteInput = row.querySelector('.stock-note-input');
				if (qtyInput) qtyInput.value = '';
				if (noteInput) noteInput.value = '';
			});
		});

		// --- NEW: Handle Stock Submission via Hidden Form ---
		manageStockModal.addEventListener('click', function(e) {
			if (e.target && e.target.classList.contains('btn-stock-submit')) {
				e.preventDefault();
				const button = e.target;
				const row = button.closest('tr');
				const action = button.dataset.action;

				// Get values from the row
				const itemId = row.dataset.itemId;
				const expirationDays = row.dataset.expirationDays;
				const qtyInput = row.querySelector('.stock-qty-input');
				const reasonSelect = row.querySelector('.reason-category-select');
				const noteInput = row.querySelector('.stock-note-input');

				const quantity = qtyInput ? qtyInput.value : '';
				const reason = reasonSelect ? reasonSelect.value : '';
				const note = noteInput ? noteInput.value : '';

				if (!quantity || parseFloat(quantity) < 0) {
					alert("Please enter a valid positive quantity.");
					return;
				}

				// Populate Hidden Form
				const hiddenForm = document.getElementById('inventoryStockForm');
				document.getElementById('hiddenItemId').value = itemId;
				document.getElementById('hiddenQuantity').value = quantity;
				document.getElementById('hiddenAction').value = action;
				document.getElementById('hiddenReasonCategory').value = reason;
				document.getElementById('hiddenReasonNote').value = note;
				document.getElementById('hiddenExpirationDays').value = expirationDays;

				// Submit Hidden Form
				hiddenForm.submit();
			}
		});

		manageStockModal.addEventListener('hidden.bs.modal', function() {
			mainElement.removeAttribute('data-show-manage-stock-modal');
		});
	}

	// Helper function for date calculations/formatting
	function formatDate(dateString) {
		if (!dateString) return 'N/A';
		try {
			const date = new Date(dateString + 'T00:00:00');
			if (isNaN(date)) return 'N/A';
			return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
		} catch (e) {
			return 'N/A';
		}
	}

	function calculateExpirationDate(receivedDateString, expirationDays) {
		if (!receivedDateString || !expirationDays || parseInt(expirationDays) <= 0) return 'No Expiration';
		try {
			const receivedDate = new Date(receivedDateString + 'T00:00:00');
			if (isNaN(receivedDate)) return 'N/A';

			const expDays = parseInt(expirationDays);
			const expirationDate = new Date(receivedDate);
			expirationDate.setDate(receivedDate.getDate() + expDays);

			const rawDateString = expirationDate.toISOString().split('T')[0];
			return formatDate(rawDateString);

		} catch (e) {
			return 'N/A';
		}
	}


	// --- viewItemModal (Custom) ---
	const viewItemModal = document.getElementById('viewItemModal');
	if (viewItemModal) {
		viewItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('view-item-btn')) {
				console.warn("View Item Modal opened without a valid row source.");
				return;
			}

			const dataset = button.dataset;

			const setText = (id, value) => {
				const el = viewItemModal.querySelector(id);
				if (el) {
					el.textContent = value || 'N/A';
				} else {
					console.warn(`Element ${id} not found in viewItemModal.`);
				}
			};

			setText('#viewItemName', dataset.name);
			setText('#viewItemCategory', dataset.categoryName);
			setText('#viewItemUnit', dataset.unitName);
			setText('#viewItemCurrentStock', dataset.currentStock);
			setText('#viewItemCostPerUnit', '₱' + (dataset.costPerUnit || '0.00'));
			setText('#viewItemTotalCostValue', '₱' + (dataset.totalCostValue || '0.00'));
			setText('#viewItemLowThreshold', dataset.lowThreshold);
			setText('#viewItemCriticalThreshold', dataset.criticalThreshold);

			const lastUpdated = (dataset.lastUpdated && dataset.lastUpdated !== 'N/A') ? dataset.lastUpdated.split(' • ')[0] : 'N/A';

			const lastUpdatedEl = viewItemModal.querySelector('#viewItemLastUpdated');
			if (lastUpdatedEl) {
				lastUpdatedEl.textContent = lastUpdated;
			}

			setText('#viewItemActiveStatus', dataset.itemStatus || 'N/A');

			const statusBadge = viewItemModal.querySelector('#viewItemStatusBadge');
			if (statusBadge) {
				statusBadge.textContent = dataset.stockStatus || 'N/Access-Denied';
				statusBadge.className = 'status-badge';
				if (dataset.stockStatusClass) {
					statusBadge.classList.add(dataset.stockStatusClass);
				}
			}

			const receivedDate = dataset.receivedDate ? formatDate(dataset.receivedDate) : 'N/A';
			const expirationDays = dataset.expirationDays || '0';

			let expirationDateText = 'No Expiration';
			if (dataset.expirationDate && dataset.expirationDate !== 'null' && dataset.expirationDate !== 'N/A') {
				expirationDateText = formatDate(dataset.expirationDate);
			} else {
				expirationDateText = calculateExpirationDate(dataset.receivedDate, dataset.expirationDays);
			}

			setText('#viewItemReceivedDate', receivedDate);
			setText('#viewItemExpirationDays', expirationDays + ' days');
			setText('#viewItemExpirationDate', expirationDateText);

			const expDateEl = viewItemModal.querySelector('#viewItemExpirationDate');
			if (expDateEl) {
				if (parseInt(expirationDays) > 0) {
					const now = new Date();
					now.setHours(0, 0, 0, 0);

					let expDateObj = null;
					if (dataset.expirationDate) {
						expDateObj = new Date(dataset.expirationDate + 'T00:00:00');
					} else if (dataset.receivedDate && parseInt(expirationDays) > 0) {
						expDateObj = new Date(dataset.receivedDate + 'T00:00:00');
						expDateObj.setDate(expDateObj.getDate() + parseInt(expirationDays));
					}

					if (expDateObj) {
						expDateObj.setHours(0, 0, 0, 0);
						if (expDateObj < now) {
							expDateEl.className = 'fw-bold text-danger';
						} else {
							expDateEl.className = 'fw-bold text-danger';
						}
					}
				} else {
					expDateEl.className = 'fw-bold text-success';
					expDateEl.textContent = 'No Expiration';
				}
			}
		});
	}
});