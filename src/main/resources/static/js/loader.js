(function() {
  "use strict";

  // Function to hide the preloader
  function hidePreloader() {
    document.body.classList.add('loaded');
  }

  // Hide preloader on window load event
  window.addEventListener('load', function() {
    console.log("Window loaded. Hiding preloader.");
    // We use requestAnimationFrame to ensure the browser is ready to paint
    requestAnimationFrame(hidePreloader);
  });

  // Failsafe: If the load event somehow fails or takes too long (e.g., > 5s), 
  // hide the loader anyway to prevent users from getting stuck.
  setTimeout(function() {
    if (!document.body.classList.contains('loaded')) {
      console.warn("Preloader failsafe triggered after 5 seconds.");
      hidePreloader();
    }
  }, 5000);

})();