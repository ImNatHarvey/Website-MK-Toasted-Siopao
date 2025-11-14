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
		const checkoutButton = cartSummary.querySelector('.btn-checkout'); // This is the button in the sidebar

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
			saveCart({});
			renderCart();
		};
		
		// --- ADDED: Helper to get total price ---
		const getCartTotal = (cart) => {
			let total = 0;
			Object.keys(cart).forEach(id => {
				const item = cart[id];
				total += item.price * item.quantity;
			});
			return total;
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
			const totalPrice = getCartTotal(cart); // Use helper

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

					const itemEl = document.createElement('div');
					itemEl.className = 'order-item';
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
		document.addEventListener('click', function(event) {
			const button = event.target.closest('.qty-btn');
			if (!button) return;

			const card = button.closest('.product-card-public');
			if (!card) return;

			const stepper = button.closest('.quantity-stepper-inline');
			if (!stepper) return;

			const qtyInput = stepper.querySelector('.qty-input');
			const minusBtn = stepper.querySelector('.qty-btn.minus');
			const plusBtn = stepper.querySelector('.qty-btn.plus');
			
			const stock = parseInt(card.dataset.productStock, 10);
			if (isNaN(stock)) {
				console.warn("Product stock is not a number:", card.dataset.productStock);
				return;
			}

			let currentQty = parseInt(qtyInput.value, 10);

			if (button.classList.contains('plus')) {
				if (currentQty < stock) {
					currentQty++;
				}
			} else if (button.classList.contains('minus')) {
				currentQty--;
			}

			if (currentQty < 0) currentQty = 0;
			
			qtyInput.value = currentQty;
			minusBtn.disabled = (currentQty === 0);
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
			const plusBtn = card.querySelector('.qty-btn.plus'); 
			const quantityToAdd = parseInt(qtyInput.value, 10);

			if (quantityToAdd > 0) {
				addToCart(productId, productName, productPrice, productImage, quantityToAdd);
				qtyInput.value = 0;
				minusBtn.disabled = true;
				if (plusBtn) {
					plusBtn.disabled = false;
				}
			}
		});

		// 13. NEW Event Listener: Handle Clicks INSIDE the Cart Summary
		cartSummary.addEventListener('click', function(event) {
			const button = event.target.closest('button');
			if (!button) return;

			const productId = button.dataset.productId;

			if (button.classList.contains('cart-increment-btn')) {
				incrementItem(productId);
			} else if (button.classList.contains('cart-decrement-btn')) {
				decrementItem(productId);
			} else if (button.classList.contains('cart-remove-btn')) {
				removeItem(productId);
			} else if (button.classList.contains('btn-clear-cart')) {
				clearCart();
			}
		});

		// 14. Initialization
		renderCart();
		
		// --- ADDED: Check for 'clearCart' flag from redirect ---
		const messageContainer = document.getElementById('toast-messages');
		if (messageContainer && messageContainer.dataset.clearCart === 'true') {
			console.log("Order success flag detected. Clearing cart.");
			clearCart();
			delete messageContainer.dataset.clearCart;
		}
		// --- END ADDED ---

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

		const messageContainer = document.getElementById('toast-messages');
		if (messageContainer) {
			const newMessages = messageContainer.dataset;
			let newToastsAdded = false;

			for (const key in newMessages) {
				if (Object.prototype.hasOwnProperty.call(newMessages, key) && newMessages[key]) {
					if (key === 'clearCart') {
						continue;
					}
					
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

	showToastNotifications();
	// ========================================================================
	// == END: TOAST NOTIFICATION LOGIC ==
	// ========================================================================


	// ========================================================================
	// == START: NEW CUSTOMER ORDER FORM & MODAL LOGIC ==
	// ========================================================================
	const mainElement = document.querySelector('main.container-fluid');
	const orderForm = document.getElementById('customer-order-form');
	const copyDetailsBtn = document.getElementById('copy-my-details-btn');
	const placeOrderBtn = document.getElementById('place-order-btn-trigger');
	const paymentModal = document.getElementById('paymentModal');
	
	if (mainElement && orderForm && copyDetailsBtn && placeOrderBtn && paymentModal) {
		console.log("Customer order form and modal logic initialized.");

		// Map of form IDs to main element data-c-* attribute suffixes
		const fieldMap = {
			'shipping-firstName': 'firstname',
			'shipping-lastName': 'lastname',
			'shipping-phone': 'phone',
			'shipping-email': 'email',
			'shipping-houseNo': 'houseno',
			'shipping-lotNo': 'lotno',
			'shipping-blockNo': 'blockno',
			'shipping-street': 'street',
			'shipping-barangay': 'barangay',
			'shipping-municipality': 'municipality',
			'shipping-province': 'province'
		};
		
		// Map of modal's hidden field IDs to form IDs
		const modalFieldMap = {
			'form_firstName': 'shipping-firstName',
			'form_lastName': 'shipping-lastName',
			'form_phone': 'shipping-phone',
			'form_email': 'shipping-email',
			'form_houseNo': 'shipping-houseNo',
			'form_lotNo': 'shipping-lotNo',
			'form_blockNo': 'shipping-blockNo',
			'form_street': 'shipping-street',
			'form_barangay': 'shipping-barangay',
			'form_municipality': 'shipping-municipality',
			'form_province': 'shipping-province'
		};

		// --- "Copy My Details" Button Logic ---
		copyDetailsBtn.addEventListener('click', function() {
			for (const [fieldId, dataKey] of Object.entries(fieldMap)) {
				const input = document.getElementById(fieldId);
				const dataValue = mainElement.dataset['c' + dataKey];
				if (input && dataValue && dataValue !== 'null') {
					input.value = dataValue;
				} else if (input) {
					input.value = ''; // Clear field if no profile data
				}
			}
			
			// Show a confirmation toast
			queueToast("Your profile details have been copied to the form.", false);
			showToastNotifications();
			
			// Remove focus from button
			copyDetailsBtn.blur();
		});

		// --- Validation Logic ---
		const phoneRegex = /^(09|\+639)\d{9}$/;
		const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
		
		const validateField = (field, regex = null, feedbackEl = null, emptyMsg = "This field is required.", invalidMsg = "Invalid format.") => {
			let isValid = true;
			const value = field.value.trim();
			
			// Check for 'required' attribute
			const isRequired = field.hasAttribute('required');
			
			if (!value) {
				if (isRequired) {
					feedbackEl.textContent = emptyMsg;
					field.classList.add('is-invalid');
					isValid = false;
				} else {
					field.classList.remove('is-invalid'); // Not required and empty is fine
				}
			} else if (regex && !regex.test(value)) {
				feedbackEl.textContent = invalidMsg;
				field.classList.add('is-invalid');
				isValid = false;
			} else if ( (field.id === 'shipping-houseNo' || field.id === 'shipping-lotNo' || field.id === 'shipping-blockNo') && value.length > 50) {
				// Specific length check for optional fields
				feedbackEl.textContent = `Cannot exceed 50 characters.`;
				field.classList.add('is-invalid');
				isValid = false;
			}
			else {
				field.classList.remove('is-invalid');
			}
			return isValid;
		};
		
		const validateShippingForm = () => {
			let isFormValid = true;
			
			isFormValid &= validateField(document.getElementById('shipping-firstName'), null, document.getElementById('shipping-firstName').nextElementSibling);
			isFormValid &= validateField(document.getElementById('shipping-lastName'), null, document.getElementById('shipping-lastName').nextElementSibling);
			isFormValid &= validateField(document.getElementById('shipping-street'), null, document.getElementById('shipping-street').nextElementSibling);
			isFormValid &= validateField(document.getElementById('shipping-barangay'), null, document.getElementById('shipping-barangay').nextElementSibling);
			isFormValid &= validateField(document.getElementById('shipping-municipality'), null, document.getElementById('shipping-municipality').nextElementSibling);
			isFormValid &= validateField(document.getElementById('shipping-province'), null, document.getElementById('shipping-province').nextElementSibling);
			
			isFormValid &= validateField(document.getElementById('shipping-phone'), phoneRegex, document.getElementById('shipping-phone-feedback'), "Phone number is required.", "Invalid format (e.g., 09xxxxxxxxx).");
			isFormValid &= validateField(document.getElementById('shipping-email'), emailRegex, document.getElementById('shipping-email-feedback'), "Email is required.", "Invalid email format.");
			
			// Optional fields with length check
			isFormValid &= validateField(document.getElementById('shipping-houseNo'), null, document.getElementById('shipping-houseNo').nextElementSibling);
			isFormValid &= validateField(document.getElementById('shipping-lotNo'), null, document.getElementById('shipping-lotNo').nextElementSibling);
			isFormValid &= validateField(document.getElementById('shipping-blockNo'), null, document.getElementById('shipping-blockNo').nextElementSibling);

			return isFormValid;
		};

		// --- "Place Order" Button Logic (Validation & Data Transfer) ---
		placeOrderBtn.addEventListener('click', function(event) {
			console.log("Place Order button clicked. Validating form...");
			if (!validateShippingForm()) {
				console.log("Form validation failed. Stopping modal.");
				event.preventDefault();
				event.stopPropagation();
				
				queueToast("Please fill out all required fields correctly.", true);
				showToastNotifications();
				
				const firstInvalid = orderForm.querySelector('.is-invalid');
				if (firstInvalid) {
					firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
					firstInvalid.focus();
				}
			} else {
				console.log("Form validation success. Copying data to modal.");
				// Copy data from visible form to modal's hidden form
				for (const [modalFieldId, formFieldId] of Object.entries(modalFieldMap)) {
					const modalInput = document.getElementById(modalFieldId);
					const formInput = document.getElementById(formFieldId);
					if (modalInput && formInput) {
						modalInput.value = formInput.value;
					}
				}
				// Note: The cart data and total are populated by the 'show.bs.modal' listener below
			}
		});

		// --- PAYMENT MODAL LOGIC ---
		const CART_KEY = 'mkSiopaoCart';
		const cartFormInput = document.getElementById('form_cartDataJson');
		const modalSummaryList = paymentModal.querySelector('.order-summary-modal-list');
		const modalTotalPriceElements = paymentModal.querySelectorAll('.total-price-modal');
		
		const gcashInstructions = document.getElementById('gcash-payment-instructions');
		const codInstructions = document.getElementById('cod-payment-instructions');
		const gcashRadio = document.getElementById('payment_gcash');
		const codRadio = document.getElementById('payment_cod');
		const receiptUploader = document.getElementById('receiptUploader');
		
		// 1. Setup Image Uploader
		if (receiptUploader) {
			setupImageUploader('receiptUploader');
		}

		// 2. Add listener to populate modal when shown
		paymentModal.addEventListener('show.bs.modal', function() {
			console.log("Payment modal opening. Populating cart data.");
			
			const cart = JSON.parse(sessionStorage.getItem(CART_KEY) || '{}');
			const productIds = Object.keys(cart);
			
			// 2a. Populate cart summary
			modalSummaryList.innerHTML = '';
			if (productIds.length === 0) {
				modalSummaryList.innerHTML = '<p class="text-danger">Your cart is empty.</p>';
			} else {
				productIds.forEach(id => {
					const item = cart[id];
					const itemTotal = item.price * item.quantity;
					const itemEl = document.createElement('div');
					itemEl.className = 'd-flex justify-content-between';
					itemEl.innerHTML = `
						<span>${item.quantity}x ${item.name}</span>
						<span class="fw-bold">${formatCurrency(itemTotal)}</span>
					`;
					modalSummaryList.appendChild(itemEl);
				});
			}
			
			// 2b. Populate total price
			const totalPrice = getCartTotal(cart);
			modalTotalPriceElements.forEach(el => {
				el.textContent = formatCurrency(totalPrice);
			});
			
			// 2c. Populate hidden cart JSON
			cartFormInput.value = JSON.stringify(cart);
			
			// 2d. Reset uploader and payment toggle
			if (receiptUploader) receiptUploader.resetUploader();
			
			// 2e. Clear notes field
			const notesField = document.getElementById('form_notes');
			if (notesField) notesField.value = '';
			
			// 2f. Reset to GCash default
			gcashRadio.checked = true;
			togglePaymentMethod();

			// 2g. Reset submit button
			const submitBtn = paymentModal.querySelector('button[type="submit"]');
			if (submitBtn) {
				submitBtn.disabled = false;
				submitBtn.innerHTML = 'Confirm & Place Order';
			}
		});
		
		// 3. Add listeners for payment method toggle
		const togglePaymentMethod = () => {
			if (codRadio.checked) {
				codInstructions.style.display = 'block';
				gcashInstructions.style.display = 'none';
				if (receiptUploader) receiptUploader.querySelector('.image-uploader-input').required = false;
			} else {
				codInstructions.style.display = 'none';
				gcashInstructions.style.display = 'block';
				if (receiptUploader) receiptUploader.querySelector('.image-uploader-input').required = true;
			}
		};
		
		if (gcashRadio) gcashRadio.addEventListener('change', togglePaymentMethod);
		if (codRadio) codRadio.addEventListener('change', togglePaymentMethod);
		
		
		// 4. Add listener for form validation (e.g., check for receipt)
		const paymentForm = document.getElementById('payment-form');
		if(paymentForm) {
			paymentForm.addEventListener('submit', function(event) {
				if (gcashRadio.checked) {
					const receiptInput = receiptUploader.querySelector('.image-uploader-input');
					if (!receiptInput.files || receiptInput.files.length === 0) {
						event.preventDefault();
						queueToast("Please upload your payment receipt to proceed.", true);
						showToastNotifications();
						return; // Stop submission
					}
				}
				// Disable submit button to prevent double-click
				const submitBtn = paymentForm.querySelector('button[type="submit"]');
				if (submitBtn) {
					submitBtn.disabled = true;
					submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Placing Order...';
				}
			});
		}
	}
	// ========================================================================
	// == END: CUSTOMER ORDER FORM & MODAL LOGIC ==
	// ========================================================================
});