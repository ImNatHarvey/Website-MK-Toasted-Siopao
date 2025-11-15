document.addEventListener('DOMContentLoaded', function() {
    // ========================================================================
    // == START: CUSTOMER ORDER FORM & MODAL LOGIC ==
    // ========================================================================
    const mainElement = document.querySelector('main.container-fluid');
    const orderForm = document.getElementById('customer-order-form');
    const copyDetailsBtn = document.getElementById('copy-my-details-btn');
    const placeOrderBtn = document.getElementById('place-order-btn-trigger');
    const paymentModalEl = document.getElementById('paymentModal');

    if (mainElement && orderForm && copyDetailsBtn && placeOrderBtn && paymentModalEl) {
        console.log("Customer order form and modal logic initialized.");

        const paymentModal = new bootstrap.Modal(paymentModalEl);

        // FIX 1: "Copy My Details" Button Logic
        const fieldMap = {
            'shipping-firstName': 'cFirstName',
            'shipping-lastName': 'cLastName',
            'shipping-phone': 'cPhone',
            'shipping-email': 'cEmail',
            'shipping-houseNo': 'cHouseNo',
            'shipping-lotNo': 'cLotNo',
            'shipping-blockNo': 'cBlockNo',
            'shipping-street': 'cStreet',
            'shipping-barangay': 'cBarangay',
            'shipping-municipality': 'cMunicipality',
            'shipping-province': 'cProvince'
        };

        copyDetailsBtn.addEventListener('click', function() {
            for (const [fieldId, dataKey] of Object.entries(fieldMap)) {
                const input = document.getElementById(fieldId);
                const dataValue = mainElement.dataset[dataKey]; 

                if (input && dataValue && dataValue !== 'null') {
                    input.value = dataValue;
                }
            }
            if (typeof queueToast === 'function') {
                queueToast("Your profile details have been copied to the form.", false);
                showToastNotifications();
            }
            copyDetailsBtn.blur();
        });

        // FIX 2: "Place Order" Button Validation Logic
        const phoneRegex = /^(09|\+639)\d{9}$/;
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        const validateField = (field, regex = null, feedbackEl = null, emptyMsg = "This field is required.", invalidMsg = "Invalid format.") => {
            let isValid = true;
            const value = field.value.trim();
            const isRequired = field.hasAttribute('required');

            if (!value) {
                if (isRequired) {
                    feedbackEl.textContent = emptyMsg;
                    field.classList.add('is-invalid');
                    isValid = false;
                } else {
                    field.classList.remove('is-invalid');
                }
            } else if (regex && !regex.test(value)) {
                feedbackEl.textContent = invalidMsg;
                field.classList.add('is-invalid');
                isValid = false;
            } else if ((field.id === 'shipping-houseNo' || field.id === 'shipping-lotNo' || field.id === 'shipping-blockNo') && value.length > 50) {
                feedbackEl.textContent = `Cannot exceed 50 characters.`;
                field.classList.add('is-invalid');
                isValid = false;
            } else {
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
            
            isFormValid &= validateField(document.getElementById('shipping-houseNo'), null, document.getElementById('shipping-houseNo').nextElementSibling);
            isFormValid &= validateField(document.getElementById('shipping-lotNo'), null, document.getElementById('shipping-lotNo').nextElementSibling);
            isFormValid &= validateField(document.getElementById('shipping-blockNo'), null, document.getElementById('shipping-blockNo').nextElementSibling);

            return isFormValid;
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

        // BUG 2 FIX: Manually control modal display
        placeOrderBtn.addEventListener('click', function(event) {
            console.log("Place Order button clicked. Validating form...");
            if (!validateShippingForm()) {
                console.log("Form validation failed. Stopping modal.");
                
                if (typeof queueToast === 'function') {
                    queueToast("Please fill out all required fields correctly.", true);
                    showToastNotifications();
                }
                
                const firstInvalid = orderForm.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    firstInvalid.focus();
                }
            } else {
                console.log("Form validation success. Copying data to modal.");
                for (const [modalFieldId, formFieldId] of Object.entries(modalFieldMap)) {
                    const modalInput = document.getElementById(modalFieldId);
                    const formInput = document.getElementById(formFieldId);
                    if (modalInput && formInput) {
                        modalInput.value = formInput.value;
                    }
                }
                paymentModal.show();
            }
        });

        // --- PAYMENT MODAL LOGIC (REFACTORED) ---
        // --- REMOVED: CART_KEY and cartFormInput ---
        const modalSummaryList = paymentModalEl.querySelector('.order-summary-modal-list');
        const modalTotalPriceElements = paymentModalEl.querySelectorAll('.total-price-modal');
        
        // --- Find the MAIN page's cart (rendered by Thymeleaf) ---
        const mainCartSummary = document.querySelector('.order-summary');
        const mainCartList = mainCartSummary ? mainCartSummary.querySelector('.order-items-list') : null;
        const mainTotalPrice = mainCartSummary ? mainCartSummary.querySelector('.total-price') : null;

        const gcashInstructions = document.getElementById('gcash-payment-instructions');
        const codInstructions = document.getElementById('cod-payment-instructions');
        const gcashRadio = document.getElementById('payment_gcash');
        const codRadio = document.getElementById('payment_cod');
        const receiptUploader = document.getElementById('receiptUploader');
        
        const transactionIdInput = document.getElementById('form_transactionId');
        const txIdFeedback = document.getElementById('transactionId-feedback');
        const receiptFeedback = document.getElementById('receipt-feedback');
        
        // --- REMOVED: formatCurrency, getCartTotal (no longer needed here) ---

        if (receiptUploader && typeof setupImageUploader === 'function') {
            setupImageUploader('receiptUploader');
        }

        paymentModalEl.addEventListener('show.bs.modal', function() {
            console.log("Payment modal opening. Populating cart data from main page.");
            
            // --- MODIFICATION: Read from main page's sidebar ---
            if (mainCartList && mainTotalPrice) {
                // Clone the cart items from the sidebar into the modal
                modalSummaryList.innerHTML = mainCartList.innerHTML;
                
                // --- Post-process: Simplify the cloned items for the modal ---
                modalSummaryList.querySelectorAll('.order-item').forEach(item => {
                    const title = item.querySelector('.order-item-title')?.textContent || 'Unknown Item';
                    const quantity = item.querySelector('.qty-input')?.value || '0';
                    const total = item.querySelector('.order-item-total')?.textContent || '₱0.00';
                    
                    const simpleItemEl = document.createElement('div');
                    simpleItemEl.className = 'd-flex justify-content-between';
                    simpleItemEl.innerHTML = `
                        <span>${quantity}x ${title}</span>
                        <span class="fw-bold">${total}</span>
                    `;
                    item.replaceWith(simpleItemEl); // Replace complex item with simple one
                });
                
                // Copy the total price
                const totalPriceText = mainTotalPrice.textContent;
                modalTotalPriceElements.forEach(el => {
                    el.textContent = totalPriceText;
                });
                
            } else {
                 modalSummaryList.innerHTML = '<p class="text-danger">Error: Cart not found.</p>';
                 modalTotalPriceElements.forEach(el => {
                    el.textContent = '₱0.00';
                });
            }
            // --- END MODIFICATION ---
            
            // --- REMOVED: cartFormInput.value = ... ---
            
            if (receiptUploader) receiptUploader.resetUploader();
            
            const notesField = document.getElementById('form_notes');
            if (notesField) notesField.value = document.getElementById('customer-order-form').querySelector('#notes')?.value || '';
            
            gcashRadio.checked = true;
            togglePaymentMethod();

            const submitBtn = paymentModalEl.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = 'Confirm & Place Order';
            }
        });
        
        const togglePaymentMethod = () => {
			const receiptInput = receiptUploader.querySelector('.image-uploader-input');
            if (codRadio.checked) {
                codInstructions.style.display = 'block';
                gcashInstructions.style.display = 'none';
                if (receiptInput) receiptInput.required = false;
                if (transactionIdInput) transactionIdInput.required = false;
                
                if (txIdFeedback) txIdFeedback.classList.remove('d-block');
                if (transactionIdInput) transactionIdInput.classList.remove('is-invalid');
                if (receiptFeedback) receiptFeedback.classList.remove('d-block');
                if (receiptUploader) receiptUploader.classList.remove('is-invalid');
                
            } else {
                codInstructions.style.display = 'none';
                gcashInstructions.style.display = 'block';
                if (receiptInput) receiptInput.required = true;
                if (transactionIdInput) transactionIdInput.required = true;
            }
        };
        
        if (gcashRadio) gcashRadio.addEventListener('change', togglePaymentMethod);
        if (codRadio) codRadio.addEventListener('change', togglePaymentMethod);
        
        const paymentForm = document.getElementById('payment-form');
        if(paymentForm) {
            paymentForm.addEventListener('submit', function(event) {
				let validationFailed = false;
				
				if (txIdFeedback) txIdFeedback.classList.remove('d-block');
                if (transactionIdInput) transactionIdInput.classList.remove('is-invalid');
                if (receiptFeedback) receiptFeedback.classList.remove('d-block');
                if (receiptUploader) receiptUploader.classList.remove('is-invalid');
				
                if (gcashRadio.checked) {
                    const receiptInput = receiptUploader.querySelector('.image-uploader-input');
                    
                    if (!receiptInput.files || receiptInput.files.length === 0) {
                        if (typeof queueToast === 'function') {
                            queueToast("Please upload your payment receipt to proceed.", true);
                        }
                        if (receiptFeedback) {
							receiptFeedback.textContent = "Please upload a screenshot of your receipt.";
							receiptFeedback.classList.add('d-block');
						}
						if (receiptUploader) receiptUploader.classList.add('is-invalid');
                        validationFailed = true;
                    }
                    
                    // --- THIS IS THE FIX ---
                    const txIdValue = transactionIdInput.value.trim();
                    const txIdRegex = /^\d{13}$/; // Regex for exactly 13 digits

                    if (txIdValue.length === 0) {
						if (typeof queueToast === 'function') {
                            queueToast("Please enter the GCash Transaction ID.", true);
                        }
                        transactionIdInput.classList.add('is-invalid');
                        if(txIdFeedback) {
							txIdFeedback.textContent = "Transaction ID is required for GCash.";
							txIdFeedback.classList.add('d-block');
						}
                        validationFailed = true;
					} else if (!txIdRegex.test(txIdValue)) { // Test against the regex
						if (typeof queueToast === 'function') {
                            queueToast("Invalid Transaction ID. It must be 13 digits.", true);
                        }
                        transactionIdInput.classList.add('is-invalid');
                        if(txIdFeedback) {
							txIdFeedback.textContent = "Invalid Transaction ID. Must be 13 digits.";
							txIdFeedback.classList.add('d-block');
						}
                        validationFailed = true;
					}
                    // --- END FIX ---
                }
                
                if (validationFailed) {
					event.preventDefault();
					if (typeof showToastNotifications === 'function') {
						showToastNotifications();
					}
					return;
				}
                
                const submitBtn = paymentForm.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Placing Order...';
                }
            });
        }
    }
});