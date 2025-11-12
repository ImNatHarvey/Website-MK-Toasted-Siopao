document.addEventListener('DOMContentLoaded', function() {
	console.log("Main script.js loaded.");

	const forceRemoveAllModalState = () => {
		const backdrops = document.querySelectorAll('.modal-backdrop');
		let removedCount = 0;
		if (backdrops.length > 0) {
			backdrops.forEach(backdrop => { backdrop.remove(); removedCount++; });
		}
		let bodyHadClass = false;
		if (document.body.classList.contains('modal-open')) {
			document.body.classList.remove('modal-open'); bodyHadClass = true;
		}
		document.body.style.overflow = ''; document.body.style.paddingRight = '';
		if (removedCount > 0 || bodyHadClass) { console.warn(`Force cleanup: Removed ${removedCount} backdrops. Reset body state.`); }
	};

	const urlParams = new URLSearchParams(window.location.search);
	const modalToShow = urlParams.get('showModal');

	if (modalToShow) {
		console.log(`URL parameter 'showModal' found: ${modalToShow}`);
		forceRemoveAllModalState();

		const checkBootstrapAndShow = () => {
			if (typeof bootstrap !== 'undefined' && bootstrap.Modal && bootstrap.Modal.getInstance) {
				console.log(`Bootstrap ready. Finding: #${modalToShow}`);
				const modalElement = document.getElementById(modalToShow);

				if (modalElement) {
					console.log(`Element #${modalToShow} found.`);
					try {
						forceRemoveAllModalState();

						const modalInstance = bootstrap.Modal.getOrCreateInstance(modalElement);

						if (modalInstance) {
							console.log(`Instance for #${modalToShow} obtained.`);

							const standardCleanupOnHide = () => {
								console.log(`hidden.bs.modal triggered for #${modalToShow}. Cleaning up.`);
								setTimeout(forceRemoveAllModalState, 50);
								modalElement.removeEventListener('hidden.bs.modal', standardCleanupOnHide);
							};

							const manageStockCleanupOnHide = () => {
								console.log(`hidden.bs.modal triggered for manageStockModal. AGGRESSIVE Cleaning up.`);
								setTimeout(forceRemoveAllModalState, 10);
								setTimeout(forceRemoveAllModalState, 60);
								setTimeout(forceRemoveAllModalState, 120);
								modalElement.removeEventListener('hidden.bs.modal', manageStockCleanupOnHide);
							};

							modalElement.removeEventListener('hidden.bs.modal', standardCleanupOnHide);
							modalElement.removeEventListener('hidden.bs.modal', manageStockCleanupOnHide);

							if (modalToShow === 'manageStockModal') {
								modalElement.addEventListener('hidden.bs.modal', manageStockCleanupOnHide);
								console.log("Attached AGGRESSIVE cleanup listener to manageStockModal.");
							} else {
								modalElement.addEventListener('hidden.bs.modal', standardCleanupOnHide);
								console.log("Attached standard cleanup listener.");
							}

							modalElement.addEventListener('shown.bs.modal', () => {
								console.log(`#${modalToShow} fully shown. Applying manual validation styles...`);
								const errorMessages = modalElement.querySelectorAll('.invalid-feedback');

								errorMessages.forEach(msg => {
									if (msg.textContent.trim() !== '') {
										console.log("Found error message:", msg.textContent.trim());

										msg.classList.add('d-block');

										let el = msg.previousElementSibling;

										if (el && el.classList.contains('form-text')) {
											el = el.previousElementSibling;
										}

										if (el) {
											if (el.classList.contains('form-control') || el.classList.contains('form-select')) {
												el.classList.add('is-invalid');
												console.log("Applying 'is-invalid' to:", el);
											} else if (el.classList.contains('input-group')) {
												el.classList.add('is-invalid');
												const inputInside = el.querySelector('.form-control, .form-select');
												if (inputInside) {
													inputInside.classList.add('is-invalid');
													console.log("Applying 'is-invalid' to group and input inside:", el);
												}
											} else if (el.classList.contains('d-flex')) {
												const inputInside = el.querySelector('.form-control.threshold-input');
												if (inputInside) {
													inputInside.classList.add('is-invalid');
													console.log("Applying 'is-invalid' to threshold input:", inputInside);
												}
											} else {
												console.warn("Could not find matching input for error message:", msg.textContent.trim());
											}
										} else {
											console.warn("No previous sibling found for error message:", msg.textContent.trim());
										}
									}
								});
							}, { once: true });

							console.log(`Calling show() for #${modalToShow}...`);
							modalInstance.show();
							console.log(`show() called for #${modalToShow}.`);

							try {
								const nextURL = window.location.pathname + window.location.search.replace(/[\?&]showModal=[^&]+/, '').replace(/^&/, '?');
								history.replaceState(null, '', nextURL);
								console.log("Cleaned URL parameter.");
							} catch (e) { console.warn("Could not clean URL", e); }
						} else { console.error(`Failed getOrCreateInstance for #${modalToShow}`); }
					} catch (e) { console.error(`Error showing modal ${modalToShow}:`, e); }
				} else { console.warn(`Element #${modalToShow} NOT FOUND.`); }
			} else {
				console.warn("Bootstrap API not ready, retrying...");
				setTimeout(checkBootstrapAndShow, 100);
			}
		};
		setTimeout(checkBootstrapAndShow, 50);

	} else {
		console.log("No 'showModal' URL parameter found.");
	}

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

	let formToSubmit = null;

	const confirmDeleteModalEl = document.getElementById('confirmDeleteModal');
	if (confirmDeleteModalEl) {
		console.log("Delete confirmation modal found. Attaching listeners.");
		const confirmDeleteModal = new bootstrap.Modal(confirmDeleteModalEl);
		const confirmDeleteButton = document.getElementById('confirmDeleteButton');
		const confirmDeleteMessage = document.getElementById('confirmDeleteMessage');

		confirmDeleteButton.addEventListener('click', function() {
			if (formToSubmit) {
				formToSubmit.submit();
				formToSubmit = null;
			}
		});

		confirmDeleteModalEl.addEventListener('hidden.bs.modal', function() {
			formToSubmit = null;
		});

		document.addEventListener('submit', function(event) {
			const form = event.target;
			if (form.dataset.confirmMessage) {
				event.preventDefault();
				formToSubmit = form;
				const message = form.dataset.confirmMessage || 'Are you sure?';
				confirmDeleteMessage.textContent = message;
				confirmDeleteModal.show();
			}
		});

	} else {
		console.log("Delete confirmation modal not found (this is normal on non-admin pages).");
	}

	const confirmSaveModalEl = document.getElementById('confirmSaveModal');
	if (confirmSaveModalEl) {
		console.log("Save confirmation modal found. Attaching listeners.");
		const confirmSaveModal = new bootstrap.Modal(confirmSaveModalEl);
		const confirmSaveButton = document.getElementById('confirmSaveButton');
		const confirmSaveMessage = document.getElementById('confirmSaveMessage');

		confirmSaveButton.addEventListener('click', function() {
			if (formToSubmit) {
				formToSubmit.submit();
				formToSubmit = null;
			}
		});

		confirmSaveModalEl.addEventListener('hidden.bs.modal', function() {
			formToSubmit = null;
		});

		document.addEventListener('submit', function(event) {
			const form = event.target;

			if (form.dataset.confirmSaveMessage) {
				event.preventDefault();
				formToSubmit = form;
				const message = form.dataset.confirmSaveMessage || 'Are you sure?';
				confirmSaveMessage.textContent = message;
				confirmSaveModal.show();
			}
		});

	} else {
		console.log("Save confirmation modal not found (this is normal on non-admin pages).");
	}

	// ========================================================================
	// == START: PUBLIC CART LOGIC (REFACTORED for Additive Cart) ==
	// ========================================================================
	const cartSummary = document.querySelector('.order-summary');
	if (cartSummary) {
		console.log("Cart summary found. Initializing public cart logic.");
		const CART_KEY = 'mkSiopaoCart';
		const orderItemsList = cartSummary.querySelector('.order-items-list');
		const emptyOrderDiv = cartSummary.querySelector('.empty-order');
		const totalPriceEl = cartSummary.querySelector('.total-price');
		const checkoutButton = cartSummary.querySelector('.btn-checkout');

		// 1. Helper: Get Cart from Session Storage
		const getCart = () => {
			try {
				const cart = JSON.parse(sessionStorage.getItem(CART_KEY) || '{}');
				return cart;
			} catch (e) {
				console.error("Failed to parse cart from sessionStorage", e);
				return {};
			}
		};

		// 2. Helper: Save Cart to Session Storage
		const saveCart = (cart) => {
			sessionStorage.setItem(CART_KEY, JSON.stringify(cart));
		};

		// 3. Helper: Format Currency
		const formatCurrency = (value) => {
			return new Intl.NumberFormat('en-PH', {
				style: 'currency',
				currency: 'PHP'
			}).format(value);
		};

		// 4. Main Function: Add to Cart
		// Adds a specified quantity to the cart (or updates existing)
		const addToCart = (productId, name, price, image, quantityToAdd) => {
			if (quantityToAdd <= 0) return; // Don't add 0 items

			const cart = getCart();
			const existingItem = cart[productId];

			if (existingItem) {
				// Item already in cart, just add quantity
				existingItem.quantity += quantityToAdd;
			} else {
				// New item
				cart[productId] = { name, price: parseFloat(price), image, quantity: quantityToAdd };
			}

			saveCart(cart);
			renderCart();
		};

		// 5. Main Function: Render Cart
		// Reads the cart and updates the "My Order" summary
		const renderCart = () => {
			const cart = getCart();
			const cartItems = Object.values(cart);

			if (!orderItemsList || !emptyOrderDiv || !totalPriceEl || !checkoutButton) return;

			orderItemsList.innerHTML = '';
			let totalPrice = 0;

			if (cartItems.length === 0) {
				// Show empty cart message
				emptyOrderDiv.classList.remove('d-none');
				orderItemsList.classList.add('d-none');
				checkoutButton.classList.add('disabled');
			} else {
				// Hide empty cart message and show list
				emptyOrderDiv.classList.add('d-none');
				orderItemsList.classList.remove('d-none');
				checkoutButton.classList.remove('disabled');

				cartItems.forEach(item => {
					const itemTotal = item.price * item.quantity;
					totalPrice += itemTotal;
					const itemEl = document.createElement('div');
					itemEl.className = 'order-item';
					itemEl.innerHTML = `
                        <img src="${item.image}" alt="${item.name}" class="order-item-img">
                        <div class="order-item-details">
                            <div class="order-item-title">${item.quantity}x ${item.name}</div>
                            <p class="order-item-price">${formatCurrency(item.price)}</p>
                        </div>
                        <div class="order-item-total">
                            ${formatCurrency(itemTotal)}
                        </div>
                    `;
					orderItemsList.appendChild(itemEl);
				});
			}

			// Update total price
			totalPriceEl.textContent = formatCurrency(totalPrice);
		};

		// 6. Event Listener: Handle Clicks on Stepper Buttons
		// This now ONLY updates the number in the input box.
		document.addEventListener('click', function(event) {
			const button = event.target.closest('.qty-btn');
			if (!button) return;

			const stepper = button.closest('.quantity-stepper-inline');
			if (!stepper) return;

			const qtyInput = stepper.querySelector('.qty-input');
			const minusBtn = stepper.querySelector('.qty-btn.minus');

			let currentQty = parseInt(qtyInput.value, 10);

			if (button.classList.contains('plus')) {
				currentQty++;
			} else if (button.classList.contains('minus')) {
				currentQty--;
			}

			if (currentQty < 0) currentQty = 0;

			qtyInput.value = currentQty;
			minusBtn.disabled = (currentQty === 0);
		});

		// 7. Event Listener: Handle "Add to Order" Button Click
		document.addEventListener('click', function(event) {
			const button = event.target.closest('.btn-add-to-order');
			if (!button) return;

			const card = button.closest('.product-card-public');
			if (!card) return;

			const { productId, productName, productPrice, productImage } = card.dataset;
			const qtyInput = card.querySelector('.qty-input');
			const minusBtn = card.querySelector('.qty-btn.minus');

			const quantityToAdd = parseInt(qtyInput.value, 10);

			if (quantityToAdd > 0) {
				// Add the selected quantity to the cart
				addToCart(productId, productName, productPrice, productImage, quantityToAdd);

				// Reset the stepper on the card
				qtyInput.value = 0;
				minusBtn.disabled = true;
			}
		});

		// 8. Initialization
		renderCart(); // Render cart from storage on load

	} else {
		console.log("Cart summary not found. Skipping public cart logic.");
	}
	// ========================================================================
	// == END: PUBLIC CART LOGIC ==
	// ========================================================================

});