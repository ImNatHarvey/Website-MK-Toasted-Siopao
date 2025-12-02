(function() {
	window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

	const customerGuide = {
		id: 'customers',
		sortOrder: 6, // 6th Page
		path: '/admin/customers',
		title: 'Customer Management',
		content: `
            <div class="guide-scroll-area">
                <h5 class="text-primary mb-3">Managing User Accounts</h5>
                <p>This module allows you to view, edit, and manage the customer base. You can track account status, update contact info, or ban users if necessary.</p>
                
                <hr>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-users me-2 text-info"></i>1. Customer Overview</h6>
                    <p class="small text-muted mb-2">The cards at the top provide a quick census:</p>
                    <ul class="small mb-0">
                        <li><strong>Total Customers:</strong> All registered accounts (excluding Admins).</li>
                        <li><strong>Active:</strong> Users who have logged in recently.</li>
                        <li><strong>Inactive:</strong> Users who haven't logged in for over 1 month.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-magnifying-glass me-2 text-primary"></i>2. Finding Customers</h6>
                    <p class="small text-muted mb-0">Use the search bar to find accounts by:</p>
                    <ul class="small mb-0">
                        <li><strong>Name:</strong> First or Last name.</li>
                        <li><strong>Username:</strong> Their unique login handle.</li>
                        <li><strong>Phone:</strong> Contact number.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-user-tag me-2 text-warning"></i>3. Account Statuses</h6>
                    <div class="row g-2 small">
                        <div class="col-12 d-flex align-items-center">
                            <span class="badge bg-success me-2" style="width: 80px;">ACTIVE</span>
                            <div>Normal account. Can log in and order.</div>
                        </div>
                        <div class="col-12 d-flex align-items-center">
                             <span class="badge bg-warning text-dark me-2" style="width: 80px;">INACTIVE</span>
                            <div>Automatically set if the user hasn't logged in for 30 days. They can reactivate by simply logging in.</div>
                        </div>
                         <div class="col-12 d-flex align-items-center">
                             <span class="badge bg-danger me-2" style="width: 80px;">DISABLED</span>
                            <div><strong>Banned.</strong> The user cannot log in. Set manually by an Admin.</div>
                        </div>
                    </div>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-user-gear me-2 text-success"></i>4. Managing Accounts</h6>
                     <ul class="list-group list-group-flush small">
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                            <button class="btn btn-sm btn-action-add me-3 pe-none" style="width: 80px; opacity: 1;"><i class="fa-solid fa-plus"></i> Add</button>
                            <div>Manually register a new customer (e.g., for phone-in orders).</div>
                        </li>
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                            <button class="btn btn-sm btn-action-view me-3 pe-none" style="width: 80px; opacity: 1;">View</button>
                            <div>See full profile, including exact <strong>Address</strong> and registration date.</div>
                        </li>
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                             <button class="btn btn-sm btn-action-edit me-3 pe-none" style="width: 80px; opacity: 1;">Edit</button>
                            <div>Update personal details or change status (e.g., to <strong>DISABLED</strong> to ban).</div>
                        </li>
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                             <button class="btn btn-sm btn-action-delete me-3 pe-none" style="width: 80px; opacity: 1;">Delete</button>
                            <div>Permanently removes the account. <strong>Secure Action:</strong> Requires the <em>Owner Password</em>.</div>
                        </li>
                    </ul>
                </div>
                
                <div class="alert alert-info small">
                    <i class="fa-solid fa-lightbulb me-1"></i> <strong>Tip:</strong> 
                    Use the <strong>Edit</strong> function to reset a customer's status to <em>ACTIVE</em> if they were accidentally disabled.
                </div>
            </div>
        `
	};

	window.MK_GUIDEBOOK_REGISTRY.push(customerGuide);
})();