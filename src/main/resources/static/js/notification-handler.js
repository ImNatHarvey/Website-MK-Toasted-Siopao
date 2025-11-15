document.addEventListener('DOMContentLoaded', function() {
    console.log("notification-handler.js loaded.");

	// --- CSRF TOKEN ---
	const csrfHeaderEl = document.querySelector('meta[name="_csrf_header"]');
	const csrfTokenEl = document.querySelector('meta[name="_csrf"]');
	
	const csrfHeader = csrfHeaderEl ? csrfHeaderEl.content : null;
	const csrfToken = csrfTokenEl ? csrfTokenEl.content : null;
	// --- END CSRF ---

    /**
     * Decrements all notification badges of a specific class.
     * @param {string} badgeClass - The class selector for the badges (e.g., '.admin-notification-badge')
     */
    function decrementBadge(badgeClass) {
        const badges = document.querySelectorAll(badgeClass);
        if (badges.length === 0) {
            return;
        }

        badges.forEach(badge => {
            let countText = badge.textContent.replace('+', '');
            let count = parseInt(countText, 10);

            if (count > 0) {
                count--;
            }

            if (count === 0) {
                badge.remove();
            } else {
                badge.textContent = count > 9 ? '9+' : count;
            }
        });
    }

    /**
     * Attaches a click listener to a notification list.
     * @param {string} listId - The ID of the <ul> element.
     * @param {string} badgeClass - The class of the badge to decrement.
     * @param {string} apiPath - The API endpoint to call (e.g., '/api/notifications/admin/read/')
     */
    function setupNotificationList(listId, badgeClass, apiPath) {
        const notificationList = document.getElementById(listId);
        if (!notificationList) {
            // console.log(`Notification list #${listId} not found on this page.`);
            return;
        }

        notificationList.addEventListener('click', function(event) {
            const link = event.target.closest('.notification-item');
            if (!link) {
                return;
            }

            // Prevent the browser from navigating immediately
            event.preventDefault();

            const notificationId = link.dataset.notificationId;
            const destinationUrl = link.href;
            const listItem = link.closest('li');

            if (!notificationId) {
                // Failsafe: if no ID, just navigate
                window.location.href = destinationUrl;
                return;
            }
            
            // --- THIS IS THE FIX: Added CSRF headers ---
            const headers = {
                'Content-Type': 'application/json',
            };
            if (csrfHeader && csrfToken) {
                headers[csrfHeader] = csrfToken;
            }
            // --- END FIX ---

            // Send the "mark as read" request
            fetch(`${apiPath}${notificationId}`, {
                    method: 'POST',
                    headers: headers // --- THIS IS THE FIX ---
                })
                .then(response => {
                    if (response.ok) {
                        console.log(`Marked notification ${notificationId} as read.`);
                        // Decrement the badge count
                        decrementBadge(badgeClass);
                        // Visually fade and remove the item from the list
                        if (listItem) {
                            listItem.style.transition = 'opacity 0.3s ease';
                            listItem.style.opacity = '0.5';
                        }
                    } else {
                        console.warn(`Failed to mark notification ${notificationId} as read.`);
                    }
                })
                .catch(error => {
                    console.error('Error marking notification as read:', error);
                })
                .finally(() => {
                    // Navigate to the destination after a short delay
                    // to allow the UI to update (or even if the fetch fails)
                    setTimeout(() => {
                        window.location.href = destinationUrl;
                    }, 150);
                });
        });
    }

    // Setup all 4 possible notification lists
    setupNotificationList('desktop-admin-notification-list', '.admin-notification-badge', '/api/notifications/admin/read/');
    setupNotificationList('mobile-admin-notification-list', '.admin-notification-badge', '/api/notifications/admin/read/');
    setupNotificationList('public-user-notification-list', '.user-notification-badge', '/api/notifications/user/read/');
    setupNotificationList('customer-notification-list', '.user-notification-badge', '/api/notifications/user/read/');
    
    // Admin list on public-facing page
    setupNotificationList('public-admin-notification-list', '.admin-notification-badge', '/api/notifications/admin/read/');

});