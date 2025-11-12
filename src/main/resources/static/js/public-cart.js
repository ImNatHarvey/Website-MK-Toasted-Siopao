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

		// 9. NEW Function: Clear Cart
		const clearCart = () => {
			if (confirm('Are you sure you want to clear your entire order?')) {
				saveCart({});
				renderCart();
			}
		};

		// 10. Main Function: Render Cart (HEAVILY MODIFIED)
		const renderCart = () => {
			const cart = getCart();
			const productIds = Object.keys(cart);

			if (!orderItemsList || !emptyOrderDiv || !totalPriceEl || !checkoutButton) return;

			orderItemsList.innerHTML = '';
			let totalPrice = 0;

			if (productIds.length === 0) {
				emptyOrderDiv.classList.remove('d-none');
				orderItemsList.classList.add('d-none');
				checkoutButton.classList.add('disabled');
			} else {
				emptyOrderDiv.classList.add('d-none');
				orderItemsList.classList.remove('d-none');
				checkoutButton.classList.remove('disabled');

				productIds.forEach(productId => {
					const item = cart[productId];
					const itemTotal = item.price * item.quantity;
					totalPrice += itemTotal;

					const itemEl = document.createElement('div');
					itemEl.className = 'order-item';
					itemEl.innerHTML = `
                        <div class="order-item-img-container">
                            <img src="${item.image}" alt="${item.name}" class="order-item-img">
                        </div>
                        <div class="order-item-details">
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
                        </div>
                        <div class="order-item-actions-vertical">
                            <div class="order-item-total">
                                ${formatCurrency(itemTotal)}
                            </div>
                            <button class="btn btn-sm btn-link text-danger cart-remove-btn" data-product-id="${productId}" title="Remove item">
                                <i class="fa-solid fa-trash-can"></i>
                            </button>
                        </div>
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

			// Only act on steppers INSIDE a product card, not the cart
			const card = button.closest('.product-card-public');
			if (!card) return;

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

		// 12. Event Listener: Handle "Add to Order" Button Click
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
				addToCart(productId, productName, productPrice, productImage, quantityToAdd);
				qtyInput.value = 0;
				minusBtn.disabled = true;
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

		// 14. Initialization
		renderCart();

	} else {
		console.log("Cart summary not found. Skipping public cart logic.");
	}
	// ========================================================================
	// == END: PUBLIC CART LOGIC ==
	// ========================================================================
});