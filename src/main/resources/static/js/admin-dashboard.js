/**
 * JavaScript for the new Admin Dashboard (admin/dashboard.html)
 * Handles parsing data from the DOM and initializing all charts.
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-dashboard.js loaded");

	// Find the main dashboard element which holds all our data
	const dashboardElement = document.getElementById('admin-dashboard-content');
	if (!dashboardElement) {
		console.error("Dashboard content element not found!");
		return;
	}

	// Helper function to parse JSON data from data-attributes
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

	// Helper function to format currency
	const formatCurrency = (value) => {
		return new Intl.NumberFormat('en-PH', {
			style: 'currency',
			currency: 'PHP'
		}).format(value);
	};

	// 1. Initialize Sales Trend Line Chart
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
					backgroundColor: 'rgba(23, 162, 184, 0.2)', // --info with alpha
					borderColor: 'rgba(23, 162, 184, 1)', // --info
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

	// 2. Initialize Order Status Donut Chart
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

		new Chart(ctx, {
			type: 'doughnut',
			data: {
				labels: labels,
				datasets: [{
					label: 'Order Status',
					data: data,
					backgroundColor: [
						'rgba(255, 193, 7, 0.8)',   // Pending (Warning)
						'rgba(23, 162, 184, 0.8)',  // Processing (Info)
						'rgba(40, 167, 69, 0.8)',  // Delivered (Success)
						'rgba(220, 53, 69, 0.8)',  // Cancelled (Danger)
						'rgba(108, 117, 125, 0.8)' // Other (Muted)
					],
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

	// 3. Initialize Top Selling Products Bar Chart
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
						'rgba(17, 63, 103, 0.7)',  // --primary
						'rgba(255, 195, 116, 0.7)', // --accent
						'rgba(23, 162, 184, 0.7)',  // --info
						'rgba(253, 245, 170, 0.7)', // --secondary
						'rgba(33, 52, 72, 0.7)'   // --dark
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
				indexAxis: 'y', // Make it a horizontal bar chart
				responsive: true,
				maintainAspectRatio: false,
				scales: {
					x: {
						beginAtZero: true,
						ticks: {
							stepSize: 1 // Ensure we only count in whole numbers
						}
					}
				},
				plugins: {
					legend: {
						display: false // Hide legend, as the axis labels are clear
					}
				}
			}
		});
	};

	// --- Initialize all charts ---
	initSalesChart();
	initOrderStatusChart();
	initTopProductsChart();

});