// Applies the saved theme before first paint. It is a file rather than an
// inline script so the site's content security policy can stay strict.
try {
  var theme = localStorage.getItem("theme");
  if (theme === "light" || theme === "dark") {
    document.documentElement.dataset.theme = theme;
  }
} catch (e) {
  // Storage can be blocked. The page then follows the device setting.
}
