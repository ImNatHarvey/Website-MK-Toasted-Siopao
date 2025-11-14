document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-dashboard.js loaded");

	const dashboardElement = document.getElementById('admin-dashboard-content');
	if (!dashboardElement) {
		console.error("Dashboard content element not found!");
		return;
	}

	const getChartData = (element, attributeName) => {
		try {
			const data = element.dataset[attributeName];
			if (!data) {
				console.warn(`Data attribute 'data-${attributeName}' is missing or empty.`);
				return null;
			}
			return JSON.parse(data);
		} catch (e) {
			console.error(`Failed to parse JSON from 'data-${attributeName}':`, e);
			return null;
		}
	};

	const formatCurrency = (value) => {
		return new Intl.NumberFormat('en-PH', {
			style: 'currency',
			currency: 'PHP'
		}).format(value);
	};

	const initSalesChart = () => {
		const ctx = document.getElementById('salesChart');
		if (!ctx) {
			console.log("Sales chart canvas not found.");
			return;
		}

		const labels = getChartData(dashboardElement, 'salesLabels');
		const data = getChartData(dashboardElement, 'salesData');

		if (!labels || !data) {
			console.warn("Missing data for Sales Chart.");
			return;
		}

		new Chart(ctx, {
			type: 'line',
			data: {
				labels: labels,
				datasets: [{
					label: 'Daily Sales',
					data: data,
					fill: true,
					backgroundColor: 'rgba(23, 162, 184, 0.2)',
					borderColor: 'rgba(23, 162, 184, 1)',
					tension: 0.1,
					pointRadius: 4,
					pointBackgroundColor: 'rgba(23, 162, 184, 1)',
				}]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				scales: {
					y: {
						beginAtZero: true,
						ticks: {
							callback: function(value) {
								return formatCurrency(value);
							}
						}
					}
				},
				plugins: {
					tooltip: {
						callbacks: {
							label: function(context) {
								let label = context.dataset.label || '';
								if (label) {
									label += ': ';
								}
								if (context.parsed.y !== null) {
									label += formatCurrency(context.parsed.y);
								}
								return label;
							}
						}
					}
				}
			}
		});
	};

	const initOrderStatusChart = () => {
		const ctx = document.getElementById('orderStatusChart');
		if (!ctx) {
			console.log("Order status chart canvas not found.");
			return;
		}

		const labels = getChartData(dashboardElement, 'orderStatusLabels');
		const data = getChartData(dashboardElement, 'orderStatusData');

		if (!labels || !data) {
			console.warn("Missing data for Order Status Chart.");
			return;
		}
		
		// --- MODIFICATION: Aligned colors with dashboard card colors ---
		const themeColors = [
			'rgba(17, 63, 103, 0.8)',   // PENDING (GCASH) - Primary Blue
			'rgba(250, 173, 20, 0.8)',  // PENDING (COD) - Warning Yellow
			'rgba(24, 144, 255, 0.8)', // PROCESSING - Info Blue
			'rgba(19, 194, 194, 0.8)', // OUT FOR DELIVERY - Teal
			'rgba(82, 196, 26, 0.8)',  // DELIVERED - Success Green
			'rgba(245, 34, 45, 0.8)',  // CANCELLED - Danger Red
			'rgba(245, 34, 45, 0.8)'   // REJECTED - Danger Red
		];
		// --- END MODIFICATION ---

		new Chart(ctx, {
			type: 'doughnut',
			data: {
				labels: labels,
				datasets: [{
					label: 'Order Status',
					data: data,
					backgroundColor: themeColors, // --- MODIFIED ---
					borderColor: '#fff',
					borderWidth: 2
				}]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				plugins: {
					legend: {
						position: 'bottom',
					}
				}
			}
		});
	};

	const initTopProductsChart = () => {
		const ctx = document.getElementById('topProductsChart');
		if (!ctx) {
			console.log("Top products chart canvas not found.");
			return;
		}

		const labels = getChartData(dashboardElement, 'topProductsLabels');
		const data = getChartData(dashboardElement, 'topProductsData');

		if (!labels || !data) {
			console.warn("Missing data for Top Products Chart.");
			return;
		}

		new Chart(ctx, {
			type: 'bar',
			data: {
				labels: labels,
				datasets: [{
					label: 'Quantity Sold',
					data: data,
					backgroundColor: [
						'rgba(17, 63, 103, 0.7)',
						'rgba(255, 195, 116, 0.7)',
						'rgba(23, 162, 184, 0.7)',
						'rgba(253, 245, 170, 0.7)',
						'rgba(33, 52, 72, 0.7)'
					],
					borderColor: [
						'rgba(17, 63, 103, 1)',
						'rgba(255, 195, 116, 1)',
						'rgba(23, 162, 184, 1)',
						'rgba(253, 245, 170, 1)',
						'rgba(33, 52, 72, 1)'
					],
					borderWidth: 1
				}]
			},
			options: {
				indexAxis: 'y',
				responsive: true,
				maintainAspectRatio: false,
				scales: {
					x: {
						beginAtZero: true,
						ticks: {
							stepSize: 1
						}
					}
				},
				plugins: {
					legend: {
						display: false
					}
				}
			}
		});
	};

	initSalesChart();
	initOrderStatusChart();
	initTopProductsChart();

});