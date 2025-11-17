document.addEventListener('DOMContentLoaded', function() {
    console.log("customer-history.js loaded.");

	// --- CSRF TOKEN ---
	const csrfHeaderEl = document.querySelector('meta[name="_csrf_header"]');
	const csrfTokenEl = document.querySelector('meta[name="_csrf"]');
	
	const csrfHeader = csrfHeaderEl ? csrfHeaderEl.content : null;
	const csrfToken = csrfTokenEl ? csrfTokenEl.content : null;
	// --- END CSRF ---
	
	const viewIssueReportModal = document.getElementById('viewIssueReportModal');
	if (viewIssueReportModal) {
		const modalLabel = viewIssueReportModal.querySelector('#viewIssueReportModalLabel');
		const modalBody = viewIssueReportModal.querySelector('#viewIssueReportModalBody');
		const loadingSpinner = viewIssueReportModal.querySelector('#viewIssueReportLoadingSpinner');
		
		viewIssueReportModal.addEventListener('show.bs.modal', async function(event) {
			const button = event.relatedTarget;
			const orderId = button.dataset.orderId;

			if (!orderId) return;
			
			// 1. Reset modal state
			modalLabel.textContent = `Issue Report for Order #ORD-${orderId}`;
			modalBody.innerHTML = ''; // Clear previous content
			modalBody.appendChild(loadingSpinner); // Add spinner back
			loadingSpinner.style.display = 'block';
			
			// 2. Fetch data from API
			try {
				const headers = { 'Content-Type': 'application/json' };
				if (csrfHeader && csrfToken) {
					headers[csrfHeader] = csrfToken;
				}
			
				const response = await fetch(`/api/issues/my-report/order/${orderId}`, {
					method: 'GET',
					headers: headers
				});
				
				if (!response.ok) {
					let errorMsg = `Failed to fetch: ${response.statusText}`;
					try {
						const errData = await response.json();
						errorMsg = errData.error || errorMsg;
					} catch (e) {
						errorMsg = "An error occurred, or you may not have permission to view this report.";
					}
					throw new Error(errorMsg);
				}
				
				const report = await response.json();
				
				// 3. Render data
				renderMyIssueReport(report, modalBody);

			} catch (error) {
				console.error("Error fetching issue report:", error);
				modalBody.innerHTML = `<div class="alert alert-danger">Could not load issue report. ${error.message}</div>`;
			} finally {
				loadingSpinner.style.display = 'none';
			}
		});
	}
	
	function renderMyIssueReport(report, container) {
		const reportedAt = report.reportedAt; 
		const statusText = report.open ? 'Open' : 'Resolved';
		const statusClass = report.open ? 'status-cancelled' : 'status-active';

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
		
		let resolutionHtml = '';
		if (!report.open) {
			const resolvedAt = report.resolvedAt; 
			const adminNotes = report.adminNotes ? report.adminNotes : 'No notes provided by admin.';
			const resolvedBy = report.resolvedByAdmin ? `Resolved by ${report.resolvedByAdmin} on ${resolvedAt}` : `Resolved on ${resolvedAt}`;
			
			resolutionHtml = `
				<hr>
				<h6 class="mb-2">Admin Resolution</h6>
				<div class="p-3 bg-light rounded" style="font-size: 0.9rem;">
					<p class="mb-1 text-muted">${adminNotes}</p>
					<p class="small text-muted mb-0">
						${resolvedBy}
					</p>
				</div>
			`;
		}

		container.innerHTML = `
			<div class="d-flex justify-content-between align-items-center mb-2">
				<h5 class="mb-0">${report.summary}</h5>
				<span class="status-badge ${statusClass}">${statusText}</span>
			</div>
			<p class="small text-muted">Reported on ${reportedAt}</p>
			
			<hr>
			
			<h6 class="mb-2">Your Report Details</h6>
			${detailsHtml}
			${attachmentHtml}
			${resolutionHtml}
		`;
	}
});