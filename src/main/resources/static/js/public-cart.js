document.addEventListener('DOMContentLoaded', function() {
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