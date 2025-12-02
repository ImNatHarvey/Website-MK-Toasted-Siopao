(function() {
	window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

	const orderGuide = {
		id: 'orders',
		sortOrder: 2, // 2nd Page
		path: '/admin/orders',
		title: 'Order Management',
		content: `
            <div class="guide-scroll-area">
                <h5 class="text-primary mb-3">Mastering Order Fulfillment</h5>
                <p>This page is your central hub for processing customer orders from reception to delivery. Efficient management here ensures happy customers.</p>
                <hr>
                
                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-filter me-2 text-primary"></i>1. Finding & Filtering</h6>
                    <p class="small text-muted mb-2">Use the tools at the top to locate specific orders:</p>
                    <ul class="small mb-0">
                        <li><strong>Status Tabs:</strong> Click tabs like <em>Pending (GCASH)</em> or <em>Processing</em> to filter the list to specific stages.</li>
                        <li><strong>Search Bar:</strong> Quickly find an order by typing the <strong>Order ID</strong> or the <strong>Customer's Name</strong>.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-tags me-2 text-warning"></i>2. Understanding Status Categories</h6>
                    <p class="small text-muted mb-2">The colored badges indicate the current stage of an order in its lifecycle:</p>
                    <div class="row g-2 small">
                        <div class="col-md-6 d-flex align-items-center">
                            <span class="badge bg-secondary me-2" style="width: 90px;">PENDING</span>
                            <div>Waiting for admin approval.</div>
                        </div>
                        <div class="col-md-6 d-flex align-items-center">
                             <span class="badge bg-info text-dark me-2" style="width: 90px;">PROCESSING</span>
                            <div>Kitchen is preparing the items.</div>
                        </div>
                         <div class="col-md-6 d-flex align-items-center">
                             <span class="badge bg-primary me-2" style="width: 90px;">DELIVERY</span>
                            <div>Rider is on the way to customer.</div>
                        </div>
                        <div class="col-md-6 d-flex align-items-center">
                             <span class="badge bg-success me-2" style="width: 90px;">DELIVERED</span>
                            <div>Transaction successfully completed.</div>
                        </div>
                         <div class="col-md-6 d-flex align-items-center">
                             <span class="badge bg-danger me-2" style="width: 90px;">CANCELLED</span>
                            <div>Order voided (Customer/Admin).</div>
                        </div>
                        <div class="col-md-6 d-flex align-items-center">
                             <span class="badge bg-danger me-2" style="width: 90px;">REJECTED</span>
                            <div>Order denied by Admin.</div>
                        </div>
                    </div>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-hand-pointer me-2 text-success"></i>3. Taking Action</h6>
                    <p class="small text-muted mb-2">Use the buttons in the "Actions" column to move orders forward:</p>
                     <ul class="list-group list-group-flush small">
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                            <button class="btn btn-sm btn-action-view me-3 pe-none" style="width: 80px; opacity: 1;">View</button>
                            <div>Opens details. You can download the <strong><i class="fa-solid fa-file-invoice"></i> Invoice</strong> or <strong><i class="fa-solid fa-receipt"></i> Receipt</strong> here.</div>
                        </li>
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                             <button class="btn btn-sm btn-action-success me-3 pe-none" style="width: 80px; opacity: 1;">Accept</button>
                            <div>Moves status to <em>PROCESSING</em>. For GCash, click 'View' first to check the receipt.</div>
                        </li>
                         <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                             <button class="btn btn-sm btn-action-delivery me-3 pe-none" style="width: 80px; opacity: 1;">Ship</button>
                            <div>Moves status to <em>OUT FOR DELIVERY</em>. Click when handing food to the rider.</div>
                        </li>
                         <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                             <button class="btn btn-sm btn-action-success me-3 pe-none" style="width: 80px; opacity: 1;">Complete</button>
                            <div>Moves status to <em>DELIVERED</em>. Click once delivery/payment is confirmed.</div>
                        </li>
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                             <button class="btn btn-sm btn-action-delete me-3 pe-none" style="width: 80px; opacity: 1;">Reject</button>
                            <div>Denies the order. <strong>Important:</strong> This automatically restocks items.</div>
                        </li>
                    </ul>
                </div>

                 <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-file-pdf me-2 text-secondary"></i>4. Documents & Records</h6>
                     <p class="small text-muted mb-2">Accessed via the <strong>View</strong> modal:</p>
                    <ul class="small mb-0">
                        <li><strong><i class="fa-solid fa-file-invoice me-1"></i> Invoice:</strong> Available immediately. Shows amount due.</li>
                        <li><strong><i class="fa-solid fa-receipt me-1"></i> Official Receipt:</strong> Available only *after* an order is marked as <strong>DELIVERED</strong>.</li>
                    </ul>
                </div>
                
                <div class="alert alert-info small">
                    <i class="fa-solid fa-lightbulb me-1"></i> <strong>Tip:</strong> 
                    Always double-check the <strong>GCash Reference No.</strong> provided by the customer against your actual GCash app transaction history before clicking "Accept".
                </div>
            </div>
        `
	};

	window.MK_GUIDEBOOK_REGISTRY.push(orderGuide);
})();