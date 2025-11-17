document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-orders.js loaded");
	
	// --- CSRF TOKEN (Copied from other JS files) ---
	const csrfHeaderEl = document.querySelector('meta[name="_csrf_header"]');
	const csrfTokenEl = document.querySelector('meta[name="_csrf"]');
	
	const csrfHeader = csrfHeaderEl ? csrfHeaderEl.content : null;
	const csrfToken = csrfTokenEl ? csrfTokenEl.content : null;
	// --- END CSRF ---

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
		
		const transactionIdContainer = viewOrderModal.querySelector('#viewTransactionIdContainer');
		const transactionIdSpan = viewOrderModal.querySelector('#viewTransactionId');
		
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
			
			if (dataset.paymentMethod === 'GCASH' && dataset.transactionId && dataset.transactionId !== 'null') {
				transactionIdSpan.textContent = dataset.transactionId;
				transactionIdContainer.style.display = 'block';
			} else {
				transactionIdSpan.textContent = '';
				transactionIdContainer.style.display = 'none';
			}

			// --- Handle Items List ---
			itemsList.innerHTML = ''; // Clear previous items
			try {
				let items = [];
				if (dataset.itemsJson && dataset.itemsJson.startsWith('[')) {
					items = JSON.parse(dataset.itemsJson);
				} else if (dataset.itemsJson) {
					items = [JSON.parse(dataset.itemsJson)];
				}

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
	
	const viewIssuesModal = document.getElementById('viewIssuesModal');
	if (viewIssuesModal) {
		const modalLabel = viewIssuesModal.querySelector('#viewIssuesModalLabel');
		const modalBody = viewIssuesModal.querySelector('#viewIssuesModalBody');
		const loadingSpinner = viewIssuesModal.querySelector('#viewIssuesLoadingSpinner');
		const listContainer = viewIssuesModal.querySelector('#viewIssuesListContainer');

		viewIssuesModal.addEventListener('show.bs.modal', async function(event) {
			const button = event.relatedTarget;
			const orderId = button.dataset.orderId;

			if (!orderId) return;
			
			// 1. Reset modal state
			modalLabel.textContent = `Issue Reports for Order #ORD-${orderId}`;
			listContainer.innerHTML = '';
			listContainer.dataset.orderId = orderId; // --- Store orderId for later ---
			loadingSpinner.style.display = 'block';

			// 2. Fetch data from API
			try {
				const response = await fetch(`/api/issues/order/${orderId}`);
				if (!response.ok) {
					let errorMsg = `Failed to fetch: ${response.statusText}`;
					try {
						const errData = await response.json();
						errorMsg = errData.error || errorMsg;
					} catch (e) {
						const textData = await response.text();
						if(textData.includes('<')) { 
							errorMsg = "An unexpected server error occurred.";
						} else {
							errorMsg = textData;
						}
					}
					throw new Error(errorMsg);
				}
				const reports = await response.json();
				
				// 3. Render data
				renderIssueReports(reports, listContainer);

			} catch (error) {
				console.error("Error fetching issue reports:", error);
				listContainer.innerHTML = `<div class="alert alert-danger">Could not load issue reports. ${error.message}</div>`;
			} finally {
				loadingSpinner.style.display = 'none';
			}
		});

		// 4. Add event listener for "Resolve" button clicks
		listContainer.addEventListener('click', async function(event) {
			const resolveButton = event.target.closest('.btn-resolve-issue');
			if (!resolveButton) return;

			event.preventDefault();
			const issueId = resolveButton.dataset.issueId;
			const card = resolveButton.closest('.card');
			const notesTextarea = card.querySelector('.admin-notes-textarea');
			const adminNotes = notesTextarea.value;

			resolveButton.disabled = true;
			resolveButton.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Resolving...';

			const headers = { 'Content-Type': 'application/json' };
			if (csrfHeader && csrfToken) {
				headers[csrfHeader] = csrfToken;
			}
			
			try {
				const response = await fetch(`/api/issues/resolve/${issueId}`, {
					method: 'POST',
					headers: headers,
					body: JSON.stringify({ adminNotes: adminNotes })
				});

				const result = await response.json();

				if (!response.ok) {
					throw new Error(result.error || 'Failed to resolve issue.');
				}

				// Success: Update the UI for this specific card
				const badge = card.querySelector('.status-badge');
				if (badge) {
					badge.classList.remove('status-cancelled');
					badge.classList.add('status-active');
					badge.textContent = 'Resolved';
				}
				card.querySelector('.card-header').classList.remove('bg-danger', 'text-white');
				card.querySelector('.card-body').style.display = 'none'; // Hide body
				card.querySelector('.card-footer').style.display = 'none'; // Hide footer
				
				// Add a "Resolved" message
				const resolvedInfo = document.createElement('div');
				resolvedInfo.className = 'card-body';
				resolvedInfo.innerHTML = `
					<p class="mb-1"><strong>Resolved by:</strong> ${result.resolvedByAdmin}</p>
					<p class="mb-1"><strong>Resolved at:</strong> ${result.resolvedAt}</p>
					<p class="mb-0"><strong>Admin Notes:</strong> ${result.adminNotes || 'N/A'}</p>
				`;
				card.appendChild(resolvedInfo);
				
				// Update the main page badge (find the button that opened the modal)
				const orderId = card.closest('#viewIssuesListContainer').dataset.orderId;
				const triggerButton = document.querySelector(`button[data-bs-target="#viewIssuesModal"][data-order-id="${orderId}"]`);
				if(triggerButton) {
					updateOpenIssueBadge(triggerButton);
				}
				

			} catch (error) {
				console.error("Error resolving issue:", error);
				alert(`Error: ${error.message}`); // Simple alert for now
				resolveButton.disabled = false;
				resolveButton.innerHTML = '<i class="fa-solid fa-check me-1"></i> Mark as Resolved';
			}
		});
	}
	
	function renderIssueReports(reports, container) {
		if (!reports || reports.length === 0) {
			container.innerHTML = '<div class="alert alert-success">No issues reported for this order.</div>';
			return;
		}

		reports.forEach(report => {
			const card = document.createElement('div');
			card.className = 'card mb-3';
			
			const reportedAt = report.reportedAt; 
			const statusText = report.open ? 'Open' : 'Resolved';
			const statusClass = report.open ? 'status-cancelled' : 'status-active';
			const headerClass = report.open ? 'bg-danger text-white' : '';

			let detailsHtml = report.details ? `<p class="card-text">${report.details}</p>` : '<p class="card-text text-muted">No details provided.</p>';
			
			let attachmentHtml = '';
			if (report.attachmentImageUrl) {
				attachmentHtml = `
					<div class="mt-3">
						<strong>Attachment:</strong><br>
						<a href="${report.attachmentImageUrl}" target="_blank">
							<img src="${report.attachmentImageUrl}" alt="Attachment" style="max-width: 200px; max-height: 200px; object-fit: contain; border: 1px solid #ddd; border-radius: 4px; margin-top: 5px;">
						</a>
					</div>
				`;
			}
			
			let footerHtml = '';
			if (report.open) {
				footerHtml = `
					<div class="card-footer">
						<div class="mb-2">
							<label for="adminNotes-${report.id}" class="form-label small"><strong>Resolution Notes (Optional):</strong></label>
							<textarea class="form-control form-control-sm admin-notes-textarea" id="adminNotes-${report.id}" rows="2" placeholder="Add notes for the customer..."></textarea>
						</div>
						<button class="btn btn-sm btn-action-success btn-resolve-issue" data-issue-id="${report.id}">
						<i class="fa-solid fa-check me-1"></i> Mark as Resolved
						</button>
					</div>
				`;
			} else {
				// Show resolution info if closed
				const resolvedAt = report.resolvedAt; 
				footerHtml = `
					<div class="card-footer bg-light">
						<p class="small mb-1"><strong>Resolved by:</strong> ${report.resolvedByAdmin || 'N/A'}</p>
						<p class="small mb-1"><strong>Resolved at:</strong> ${resolvedAt}</p>
						<p class="small mb-0"><strong>Admin Notes:</strong> ${report.adminNotes || 'N/A'}</p>
					</div>
				`;
			}

			card.innerHTML = `
				<div class="card-header d-flex justify-content-between align-items-center ${headerClass}">
					<h6 class="mb-0">Report #${report.id}: ${report.summary}</h6>
					<span class="status-badge ${statusClass}">${statusText}</span>
				</div>
				<div class="card-body" ${!report.open ? 'style="display: none;"' : ''}>
					<p class="small text-muted mb-2">Reported by ${report.username} on ${reportedAt}</p>
					${detailsHtml}
					${attachmentHtml}
				</div>
				${footerHtml}
			`;
			container.appendChild(card);
		});
	}
	
	// Helper to update the badge on the main page after resolving an issue
	function updateOpenIssueBadge(triggerButton) {
		const badge = triggerButton.querySelector('.badge');
		if (!badge) return; // No badge, nothing to do
		
		let count = parseInt(badge.textContent, 10);
		if (count > 1) {
			count--;
			badge.textContent = count;
			triggerButton.title = `View ${count} Open Issue(s)`;
		} else {
			// Last issue was resolved, remove badge and red outline
			badge.remove();
			triggerButton.classList.remove('btn-outline-danger');
			triggerButton.title = 'View Issues';
		}
	}
});