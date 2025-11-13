document.addEventListener('DOMContentLoaded', function() {
	// ========================================================================
	// == START: PUBLIC CART LOGIC (REFACTORED for Full Cart Management) ==
	// ========================================================================
	const cartSummary = document.querySelector('.order-summary');
	if (cartSummary) {
		console.log("Cart summary found. Initializing public cart logic.");
		const CART_KEY = 'mkSiopaoCart';
		const orderItemsList = cartSummary.querySelector('.order-items-list');
		const emptyOrderDiv = cartSummary.querySelector('.empty-order');
		const totalPriceEl = cartSummary.querySelector('.total-price');
		const checkoutButton = cartSummary.querySelector('.btn-checkout');

		// 1. Helper: Get Cart
		const getCart = () => {
			try {
				const cart = JSON.parse(sessionStorage.getItem(CART_KEY) || '{}');
				return cart;
			} catch (e) {
				console.error("Failed to parse cart from sessionStorage", e);
				return {};
			}
		};

		// 2. Helper: Save Cart
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

		// 4. Main Function: Add to Cart (from Product Card)
		const addToCart = (productId, name, price, image, quantityToAdd) => {
			if (quantityToAdd <= 0) return;

			const cart = getCart();
			const existingItem = cart[productId];

			if (existingItem) {
				existingItem.quantity += quantityToAdd;
			} else {
				cart[productId] = { name, price: parseFloat(price), image, quantity: quantityToAdd };
			}

			saveCart(cart);
			renderCart();
		};

		// 5. NEW Function: Update Cart Quantity (from Cart)
		const updateCartQuantity = (productId, newQuantity) => {
			const cart = getCart();
			if (!cart[productId]) return;

			if (newQuantity <= 0) {
				delete cart[productId];
			} else {
				cart[productId].quantity = newQuantity;
			}

			saveCart(cart);
			renderCart();
		};

		// 6. NEW Function: Increment Item
		const incrementItem = (productId) => {
			const cart = getCart();
			if (cart[productId]) {
				updateCartQuantity(productId, cart[productId].quantity + 1);
			}
		};

		// 7. NEW Function: Decrement Item
		const decrementItem = (productId) => {
			const cart = getCart();
			if (cart[productId]) {
				updateCartQuantity(productId, cart[productId].quantity - 1);
			}
		};

		// 8. NEW Function: Remove Item
		const removeItem = (productId) => {
			updateCartQuantity(productId, 0); // Setting qty to 0 removes it
		};

		// 9. NEW Function: Clear Cart (MODIFIED)
		const clearCart = () => {
			// if (confirm('Are you sure you want to clear your entire order?')) { // REMOVED as per user request
			saveCart({});
			renderCart();
			// } // REMOVED
		};

		// 10. Main Function: Render Cart (HEAVILY MODIFIED)
		const renderCart = () => {
			const cart = getCart();
			const productIds = Object.keys(cart);

			if (!orderItemsList || !emptyOrderDiv || !totalPriceEl || !checkoutButton) {
				console.warn("Cart elements (list, emptyDiv, price, button) not all found. Cart render aborted.");
				return;
			}

			orderItemsList.innerHTML = '';
			let totalPrice = 0;

			const isButtonTag = checkoutButton.tagName === 'BUTTON';

			if (productIds.length === 0) {
				emptyOrderDiv.classList.remove('d-none');
				orderItemsList.classList.add('d-none');

				if (isButtonTag) {
					checkoutButton.disabled = true;
				} else {
					checkoutButton.classList.add('disabled');
					checkoutButton.style.pointerEvents = 'none';
					checkoutButton.setAttribute('aria-disabled', 'true');
				}
			} else {
				emptyOrderDiv.classList.add('d-none');
				orderItemsList.classList.remove('d-none');

				if (isButtonTag) {
					checkoutButton.disabled = false;
				} else {
					checkoutButton.classList.remove('disabled');
					checkoutButton.style.pointerEvents = 'auto';
					checkoutButton.setAttribute('aria-disabled', 'false');
				}

				productIds.forEach(productId => {
					const item = cart[productId];
					const itemTotal = item.price * item.quantity;
					totalPrice += itemTotal;

					const itemEl = document.createElement('div');
					itemEl.className = 'order-item';
					// --- THIS IS THE NEW HTML STRUCTURE ---
					itemEl.innerHTML = `
                        <div class="order-item-img-container">
                            <img src="${item.image}" alt="${item.name}" class="order-item-img">
                        </div>
                        <div class="order-item-title">${item.name}</div>
                        <div class="quantity-stepper-inline" style="max-width: 90px;">
                            <button class="btn btn-sm btn-action-delete qty-btn minus cart-decrement-btn" data-product-id="${productId}">
                                <i class="fa-solid fa-minus"></i>
                            </button>
                            <input type="text" class="form-control form-control-sm qty-input" value="${item.quantity}" readonly>
                            <button class="btn btn-sm btn-action-add qty-btn plus cart-increment-btn" data-product-id="${productId}">
                                <i class="fa-solid fa-plus"></i>
                            </button>
                        </div>
                        <div class="order-item-total">
                            ${formatCurrency(itemTotal)}
                        </div>
                        <button class="btn btn-sm btn-link text-danger cart-remove-btn" data-product-id="${productId}" title="Remove item">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    `;
					orderItemsList.appendChild(itemEl);
				});
			}

			totalPriceEl.textContent = formatCurrency(totalPrice);
		};

		// 11. Event Listener: Handle Product Card Stepper
		// --- MODIFIED: Added stock limiting logic ---
		document.addEventListener('click', function(event) {
			const button = event.target.closest('.qty-btn');
			if (!button) return;

			// Only act on steppers INSIDE a product card, not the cart
			const card = button.closest('.product-card-public');
			if (!card) return;

			const stepper = button.closest('.quantity-stepper-inline');
			if (!stepper) return;

			const qtyInput = stepper.querySelector('.qty-input');
			const minusBtn = stepper.querySelector('.qty-btn.minus');
			const plusBtn = stepper.querySelector('.qty-btn.plus'); // Get plus button
			
			// Get stock from data attribute
			const stock = parseInt(card.dataset.productStock, 10);
			if (isNaN(stock)) {
				console.warn("Product stock is not a number:", card.dataset.productStock);
				return; // Do nothing if stock isn't a valid number
			}

			let currentQty = parseInt(qtyInput.value, 10);

			if (button.classList.contains('plus')) {
				// Only increment if current quantity is less than stock
				if (currentQty < stock) {
					currentQty++;
				}
			} else if (button.classList.contains('minus')) {
				currentQty--;
			}

			if (currentQty < 0) currentQty = 0;
			
			qtyInput.value = currentQty;
			
			// Disable minus button if quantity is 0
			minusBtn.disabled = (currentQty === 0);
			// Disable plus button if quantity is equal to or greater than stock
			plusBtn.disabled = (currentQty >= stock);
		});

		// 12. Event Listener: Handle "Add to Order" Button Click
		document.addEventListener('click', function(event) {
			const button = event.target.closest('.btn-add-to-order');
			if (!button) return;

			const card = button.closest('.product-card-public');
			if (!card) return;

			const { productId, productName, productPrice, productImage } = card.dataset;
			const qtyInput = card.querySelector('.qty-input');
			const minusBtn = card.querySelector('.qty-btn.minus');
			const plusBtn = card.querySelector('.qty-btn.plus'); // Get plus button
			const quantityToAdd = parseInt(qtyInput.value, 10);

			if (quantityToAdd > 0) {
				addToCart(productId, productName, productPrice, productImage, quantityToAdd);
				qtyInput.value = 0;
				minusBtn.disabled = true;
				if (plusBtn) {
					plusBtn.disabled = false; // Re-enable plus button after adding to cart
				}
			}
		});

		// 13. NEW Event Listener: Handle Clicks INSIDE the Cart Summary
		cartSummary.addEventListener('click', function(event) {
			const button = event.target.closest('button');
			if (!button) return;

			const productId = button.dataset.productId;

			if (button.classList.contains('cart-increment-btn')) {
				console.log("Cart increment:", productId);
				incrementItem(productId);
			} else if (button.classList.contains('cart-decrement-btn')) {
				console.log("Cart decrement:", productId);
				decrementItem(productId);
			} else if (button.classList.contains('cart-remove-btn')) {
				console.log("Cart remove:", productId);
				removeItem(productId);
			} else if (button.classList.contains('btn-clear-cart')) {
				console.log("Cart clear all");
				clearCart();
			}
		});

		// 14. --- THIS BLOCK WAS REMOVED as it was causing the bug ---

		// 15. Initialization (was 14)
		renderCart();

	} else {
		console.log("Cart summary not found. Skipping public cart logic.");
	}
	// ========================================================================
	// == END: PUBLIC CART LOGIC ==
	// ========================================================================


	// ========================================================================
	// == START: TOAST NOTIFICATION LOGIC (Copied from admin-utils.js) ==
	// ========================================================================
	const TOAST_QUEUE_KEY = 'toastQueue';

	const queueToast = (message, isError = false) => {
		let queue = [];
		try {
			queue = JSON.parse(sessionStorage.getItem(TOAST_QUEUE_KEY) || '[]');
		} catch (e) {
			queue = [];
		}
		const toastId = `toast-${Date.now()}-${Math.random()}`;
		queue.push({
			id: toastId,
			message: message,
			isError: isError,
			timestamp: Date.now()
		});
		sessionStorage.setItem(TOAST_QUEUE_KEY, JSON.stringify(queue));
	};

	const showToastNotifications = () => {
		const toastContainer = document.querySelector('.toast-container');
		if (!toastContainer) {
			console.error("Toast .toast-container not found in DOM. Cannot display toasts.");
			return;
		}

		let queue = [];
		try {
			queue = JSON.parse(sessionStorage.getItem(TOAST_QUEUE_KEY) || '[]');
		} catch (e) {
			console.error("Failed to parse toast queue from sessionStorage:", e);
			queue = [];
		}

		const messageContainer = document.getElementById('toast-messages'); // This div is optional
		if (messageContainer) {
			const newMessages = messageContainer.dataset;
			let newToastsAdded = false;

			for (const key in newMessages) {
				if (Object.prototype.hasOwnProperty.call(newMessages, key) && newMessages[key]) {
					const message = newMessages[key];
					const isError = key.toLowerCase().includes('error');
					const toastId = `toast-${Date.now()}-${Math.random()}`;

					queue.push({
						id: toastId,
						message: message,
						isError: isError,
						timestamp: Date.now()
					});
					newToastsAdded = true;
					delete messageContainer.dataset[key];
				}
			}
			if (newToastsAdded) {
				sessionStorage.setItem(TOAST_QUEUE_KEY, JSON.stringify(queue));
			}
		}

		toastContainer.innerHTML = '';

		queue.forEach(toastData => {
			const { id, message, isError, timestamp } = toastData;
			const toastEl = document.createElement('div');
			toastEl.id = id;
			toastEl.className = `toast`;
			toastEl.setAttribute('role', 'alert');
			toastEl.setAttribute('aria-live', 'assertive');
			toastEl.setAttribute('aria-atomic', 'true');

			const headerClass = isError ? 'text-bg-danger' : 'text-bg-success';
			const iconClass = isError ? 'fa-triangle-exclamation' : 'fa-check-circle';
			const title = isError ? 'Error' : 'Success';
			const startTime = timestamp;

			toastEl.innerHTML = `
                <div class="toast-header ${headerClass} text-white">
                    <i class="fa-solid ${iconClass} me-2"></i>
                    <strong class="me-auto">${title}</strong>
                    <small class="ms-2 toast-time">now</small>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">
                    ${message}
                </div>
            `;

			toastContainer.appendChild(toastEl);
			const toast = new bootstrap.Toast(toastEl, { autohide: false });
			const timeElement = toastEl.querySelector('.toast-time');
			let timerInterval = null;

			const updateTimer = () => {
				const now = Date.now();
				const elapsed = Math.round((now - startTime) / 1000); // seconds
				if (elapsed < 5) {
					timeElement.textContent = 'now';
				} else if (elapsed < 60) {
					timeElement.textContent = `${elapsed}s ago`;
				} else {
					const minutes = Math.floor(elapsed / 60);
					const seconds = elapsed % 60;
					timeElement.textContent = `${minutes}m ${seconds}s ago`;
				}
			};

			timerInterval = setInterval(updateTimer, 1000);
			updateTimer();

			toastEl.addEventListener('hidden.bs.toast', () => {
				if (timerInterval) clearInterval(timerInterval);
				try {
					let currentQueue = JSON.parse(sessionStorage.getItem(TOAST_QUEUE_KEY) || '[]');
					const newQueue = currentQueue.filter(t => t.id !== id);
					sessionStorage.setItem(TOAST_QUEUE_KEY, JSON.stringify(newQueue));
				} catch (e) {
					console.error("Failed to update toast queue on close:", e);
				}
				toastEl.remove();
			});

			toast.show();
		});
	};

	// Show any pending toasts on page load
	showToastNotifications();
	// ========================================================================
	// == END: TOAST NOTIFICATION LOGIC ==
	// ========================================================================


	// ========================================================================
	// == START: NEW ORDER FORM VALIDATION LOGIC ==
	// ========================================================================
	const orderForm = document.getElementById('order-form');
	const checkoutBtn = document.getElementById('proceed-to-checkout-btn');

	if (orderForm && checkoutBtn) {
		console.log("Order form and checkout button found. Attaching validation listener.");

		const validateField = (field, regex, feedbackEl, emptyMsg, invalidMsg) => {
			let isValid = true;
			if (!field.value.trim()) {
				feedbackEl.textContent = emptyMsg;
				field.classList.add('is-invalid');
				isValid = false;
			} else if (regex && !regex.test(field.value.trim())) {
				feedbackEl.textContent = invalidMsg;
				field.classList.add('is-invalid');
				isValid = false;
			} else {
				field.classList.remove('is-invalid');
			}
			return isValid;
		};

		checkoutBtn.addEventListener('click', function(event) {
			event.preventDefault();
			console.log("Checkout button clicked.");

			const formFields = {
				firstName: document.getElementById('first-name'),
				lastName: document.getElementById('last-name'),
				phone: document.getElementById('phone'),
				email: document.getElementById('email'),
				street: document.getElementById('street'),
				barangay: document.getElementById('barangay'),
				municipality: document.getElementById('municipality'),
				province: document.getElementById('province'),
				// Optional fields
				houseNo: document.getElementById('houseNo'),
				lotNo: document.getElementById('lotNo'),
				blockNo: document.getElementById('blockNo'),
				notes: document.getElementById('notes')
			};

			const phoneRegex = /^(09|\+639)\d{9}$/;
			const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

			let isFormValid = true;

			// Clear previous validation
			orderForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));

			// Validate required fields
			isFormValid &= validateField(formFields.firstName, null, formFields.firstName.nextElementSibling, "First name is required.");
			isFormValid &= validateField(formFields.lastName, null, formFields.lastName.nextElementSibling, "Last name is required.");
			isFormValid &= validateField(formFields.street, null, formFields.street.nextElementSibling, "Street / Subdivision is required.");
			isFormValid &= validateField(formFields.barangay, null, formFields.barangay.nextElementSibling, "Barangay is required.");
			isFormValid &= validateField(formFields.municipality, null, formFields.municipality.nextElementSibling, "Municipality is required.");
			isFormValid &= validateField(formFields.province, null, formFields.province.nextElementSibling, "Province is required.");

			// Validate regex fields
			isFormValid &= validateField(formFields.phone, phoneRegex, document.getElementById('phone-feedback'), "Phone number is required.", "Invalid Philippine phone number (e.g., 09xxxxxxxxx).");
			isFormValid &= validateField(formFields.email, emailRegex, document.getElementById('email-feedback'), "Email is required.", "Invalid email format.");

			// Validate optional field lengths
			[formFields.houseNo, formFields.lotNo, formFields.blockNo].forEach(field => {
				if (field.value.trim().length > 50) {
					isFormValid = false;
					field.classList.add('is-invalid');
					field.nextElementSibling.textContent = "Cannot exceed 50 characters.";
				} else {
					field.classList.remove('is-invalid');
				}
			});


			if (!isFormValid) {
				console.log("Form validation failed.");
				queueToast("Please fill out all required fields correctly.", true);
				showToastNotifications();
				// Find the first invalid field and scroll to it
				const firstInvalid = orderForm.querySelector('.is-invalid');
				if (firstInvalid) {
					firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
				}
			} else {
				console.log("Form validation successful. Proceeding to signup page.");

				// Collect data
				const guestData = {
					firstName: formFields.firstName.value.trim(),
					lastName: formFields.lastName.value.trim(),
					phone: formFields.phone.value.trim(),
					email: formFields.email.value.trim(),
					houseNo: formFields.houseNo.value.trim(),
					lotNo: formFields.lotNo.value.trim(),
					blockNo: formFields.blockNo.value.trim(),
					street: formFields.street.value.trim(),
					barangay: formFields.barangay.value.trim(),
					municipality: formFields.municipality.value.trim(),
					province: formFields.province.value.trim()
				};

				// Store in sessionStorage
				sessionStorage.setItem('guestCheckoutData', JSON.stringify(guestData));

				// Redirect to signup page
				window.location.href = '/signup?source=checkout';
			}
		});
	}
	// ========================================================================
	// == END: NEW ORDER FORM VALIDATION LOGIC ==
	// ========================================================================
});