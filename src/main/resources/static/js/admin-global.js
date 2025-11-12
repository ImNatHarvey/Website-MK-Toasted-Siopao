document.addEventListener('DOMContentLoaded', function() {
	console.log("Admin global-script.js loaded (sidebar logic).");

	// == MODIFIED SIDEBAR LOGIC ==
	const sidebarToggle = document.getElementById('sidebarToggle'); // mobile
	const desktopSidebarToggle = document.getElementById('desktopSidebarToggle'); // desktop
	const sidebarOverlay = document.getElementById('sidebarOverlay');
	const adminSidebar = document.getElementById('admin-sidebar');
	const adminBody = document.getElementById('admin-body');

	if (sidebarOverlay && adminSidebar && adminBody) {
		console.log("Mobile/Desktop sidebar elements found. Attaching listeners.");

		const toggleFunc = function() {
			adminBody.classList.toggle('sidebar-toggled');
		};

		if (sidebarToggle) {
			sidebarToggle.addEventListener('click', toggleFunc);
		}
		if (desktopSidebarToggle) { // Add listener for desktop toggle
			desktopSidebarToggle.addEventListener('click', toggleFunc);
		}

		sidebarOverlay.addEventListener('click', function() {
			adminBody.classList.remove('sidebar-toggled');
		});

		const sidebarLinks = adminSidebar.querySelectorAll('.nav-link');
		sidebarLinks.forEach(link => {
			link.addEventListener('click', function() {
				// Only auto-close on mobile
				if (window.innerWidth < 992 && adminBody.classList.contains('sidebar-toggled')) {
					adminBody.classList.remove('sidebar-toggled');
				}
			});
		});

	} else {
		console.log("Sidebar elements not found (this is normal on non-admin pages).");
	}
	// == END MODIFIED SIDEBAR LOGIC ==
});