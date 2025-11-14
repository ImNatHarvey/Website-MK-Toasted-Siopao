document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-orders.js loaded");

	const viewOrderModal = document.getElementById('viewOrderModal');
	if (viewOrderModal) {
		const modalTitle = viewOrderModal.querySelector('#viewOrderModalLabel');
		const orderTitle = viewOrderModal.querySelector('#viewOrderTitle');
		const orderStatusBadge = viewOrderModal.querySelector('#viewOrderStatusBadge');
		const orderTotal = viewOrderModal.querySelector('#viewOrderTotal');
		const orderDate = viewOrderModal.querySelector('#viewOrderDate');
		
		const customerName = viewOrderModal.querySelector('#viewCustomerName');
		const customerPhone = viewOrderModal.querySelector('#viewCustomerPhone');
		const customerAddress = viewOrderModal.querySelector('#viewCustomerAddress');
		
		const notesContainer = viewOrderModal.querySelector('#viewNotesContainer');
		const orderNotes = viewOrderModal.querySelector('#viewOrderNotes');

		const paymentMethod = viewOrderModal.querySelector('#viewPaymentMethod');
		const paymentStatus = viewOrderModal.querySelector('#viewPaymentStatus');
		
		const receiptContainer = viewOrderModal.querySelector('#viewReceiptContainer');
		const receiptLink = viewOrderModal.querySelector('#viewReceiptLink');
		const receiptImage = viewOrderModal.querySelector('#viewReceiptImage');
		
		const itemsList = viewOrderModal.querySelector('#viewOrderItemsList');
		const totalFooter = viewOrderModal.querySelector('#viewOrderTotalFooter');

		viewOrderModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('view-order-btn')) {
				console.warn("View Order Modal opened without a valid button source.");
				return;
			}

			const row = button.closest('tr');
			if (!row) {
				console.warn("Could not find parent table row for order modal.");
				return;
			}

			const dataset = row.dataset;
			console.log("Populating View Order Modal with data:", dataset);

			// --- Helper to format currency ---
			const formatCurrency = (value) => {
				if (isNaN(parseFloat(value))) return "₱0.00";
				return new Intl.NumberFormat('en-PH', {
					style: 'currency',
					currency: 'PHP'
				}).format(value);
			};

			// --- Populate Modal ---
			const orderId = dataset.orderId || 'XXXX';
			modalTitle.textContent = `Details for Order #ORD-${orderId}`;
			orderTitle.textContent = `Order #ORD-${orderId}`;
			
			const orderTotalValue = dataset.orderTotal || '0.00';
			orderTotal.textContent = `₱${orderTotalValue}`;
			totalFooter.textContent = `₱${orderTotalValue}`;
			orderDate.textContent = dataset.orderDate || 'N/A';
			
			orderStatusBadge.textContent = dataset.orderStatus || 'N/A';
			orderStatusBadge.className = 'status-badge ' + (dataset.statusClass || 'status-cancelled');
			
			customerName.textContent = dataset.customerName || 'N/A';
			customerPhone.textContent = dataset.customerPhone || 'N/A';
			customerAddress.textContent = dataset.customerAddress || 'N/A';
			
			paymentMethod.textContent = dataset.paymentMethod || 'N/A';
			paymentStatus.textContent = dataset.paymentStatus || 'N/A';

			// --- Handle Notes ---
			if (dataset.notes && dataset.notes.trim() !== '') {
				orderNotes.textContent = dataset.notes;
				notesContainer.style.display = 'block';
			} else {
				orderNotes.textContent = '';
				notesContainer.style.display = 'none';
			}

			// --- Handle GCash Receipt ---
			if (dataset.paymentMethod === 'GCASH' && dataset.receiptUrl && dataset.receiptUrl !== 'null') {
				receiptImage.src = dataset.receiptUrl;
				receiptLink.href = dataset.receiptUrl;
				receiptContainer.style.display = 'block';
			} else {
				receiptImage.src = '';
				receiptLink.href = '#';
				receiptContainer.style.display = 'none';
			}

			// --- Handle Items List ---
			itemsList.innerHTML = ''; // Clear previous items
			try {
				let items = [];
				// --- ***** THIS IS THE FIX ***** ---
				if (dataset.itemsJson && dataset.itemsJson.startsWith('[')) {
					// The data is now a valid JSON string, so we can parse it directly.
					items = JSON.parse(dataset.itemsJson);
				} else if (dataset.itemsJson) {
					// Fallback just in case, but should not be used
					items = [JSON.parse(dataset.itemsJson)];
				}
				// --- ***** END OF FIX ***** ---

				if (items.length > 0) {
					items.forEach(item => {
						const subtotal = (item.quantity || 0) * (item.price || 0);
						const tr = document.createElement('tr');
						tr.innerHTML = `
							<td>${item.name || 'Unknown Item'}</td>
							<td class="text-center">${item.quantity || 0}</td>
							<td class="text-end">${formatCurrency(item.price || 0)}</td>
							<td class="text-end">${formatCurrency(subtotal)}</td>
						`;
						itemsList.appendChild(tr);
					});
				} else {
					throw new Error("No items parsed.");
				}
				
			} catch (e) {
				console.error("Error parsing order items JSON:", e, "Data:", dataset.itemsJson);
				itemsList.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Error loading items.</td></tr>';
			}
		});
	}
});