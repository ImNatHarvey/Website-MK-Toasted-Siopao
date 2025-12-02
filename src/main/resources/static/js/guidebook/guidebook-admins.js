(function() {
	window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

	const adminGuide = {
		id: 'admins',
		sortOrder: 7, // 7th Page
		path: '/admin/admins',
		title: 'Admin Management',
		content: `
            <div class="guide-scroll-area">
                <h5 class="text-primary mb-3">Managing Team Access</h5>
                <p>This module controls who can access the admin portal and what they can do. It handles account creation, role assignment, and security permissions.</p>
                
                <hr>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-user-shield me-2 text-info"></i>1. Admin Overview</h6>
                    <p class="small text-muted mb-2">The cards at the top give a quick status report:</p>
                    <ul class="small mb-0">
                        <li><strong>Total Admins:</strong> The count of all staff accounts (including the Owner).</li>
                        <li><strong>Active:</strong> Admins who can currently log in.</li>
                        <li><strong>Inactive:</strong> Admins who haven't logged in for a while (but can still access the site).</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-shield-halved me-2 text-warning"></i>2. Roles & Permissions</h6>
                    <p class="small text-muted mb-2">Click the <span class="badge bg-transparent text-primary border border-primary">Manage Roles</span> button to configure access levels.</p>
                    <ul class="list-group list-group-flush small">
                        <li class="list-group-item bg-transparent px-0 py-1">
                            <strong>ROLE_OWNER:</strong> The Super Admin. Has full access to everything, including deleting other admins and managing site settings. Cannot be deleted.
                        </li>
                        <li class="list-group-item bg-transparent px-0 py-1">
                            <strong>Custom Roles:</strong> You can create roles like <em>SUPERVISOR</em> or <em>INVENTORY_MANAGER</em> and assign specific permissions.
                        </li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-users-gear me-2 text-success"></i>3. Account Actions</h6>
                    <p class="small text-muted mb-2">Use the buttons in the "Actions" column:</p>
                    <ul class="list-group list-group-flush small">
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                            <button class="btn btn-sm btn-action-view me-3 pe-none" style="width: 80px; opacity: 1;">View</button>
                            <div>See the admin's details and a grid of their specific <strong>Permissions</strong>.</div>
                        </li>
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                             <button class="btn btn-sm btn-action-edit me-3 pe-none" style="width: 80px; opacity: 1;">Edit</button>
                            <div>Update details or change their <strong>Role</strong>. <em class="text-muted">(Note: You cannot edit the Owner account)</em>.</div>
                        </li>
                        <li class="list-group-item d-flex align-items-center bg-transparent px-0 py-2">
                             <button class="btn btn-sm btn-action-delete me-3 pe-none" style="width: 80px; opacity: 1;">Delete</button>
                            <div>Permanently removes the admin. <strong>Security Check:</strong> This action requires the <em>Owner Password</em> to proceed.</div>
                        </li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-user-pen me-2 text-secondary"></i>4. Your Profile</h6>
                    <p class="small text-muted mb-0">Click the <strong>Edit My Profile</strong> button at the top right to update your own name, email, or change your password.</p>
                </div>
                
                <div class="alert alert-info small">
                    <i class="fa-solid fa-lightbulb me-1"></i> <strong>Tip:</strong> 
                    When creating a new role, start with the minimum permissions needed. You can always add more later by editing the role in the <em>Manage Roles</em> modal.
                </div>
            </div>
        `
	};

	window.MK_GUIDEBOOK_REGISTRY.push(adminGuide);
})();