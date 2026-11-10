// SecondHand MarketPlace - Client JavaScript

let currentListings = [];
let searchDebounceTimer = null;

// Initialization
document.addEventListener("DOMContentLoaded", function () {
  loadListings();
  loadStats();
});

// Load stats from Java Backend
function loadStats() {
  fetch("/api/stats")
    .then(function (res) { return res.json(); })
    .then(function (data) {
      document.getElementById("statActive").innerText = data.activeListings || 0;
      document.getElementById("statSold").innerText = data.soldListings || 0;
      document.getElementById("statInquiries").innerText = data.totalInquiries || 0;
      document.getElementById("statTotal").innerText = data.totalListings || 0;
    })
    .catch(function (err) {
      console.warn("Could not load stats:", err);
    });
}

// Build query string and fetch listings
function loadListings() {
  var search = document.getElementById("searchInput").value.trim();
  var category = document.getElementById("categoryFilter").value;
  var condition = document.getElementById("conditionFilter").value;
  var status = document.getElementById("statusFilter").value;
  var minPrice = document.getElementById("minPriceInput").value.trim();
  var maxPrice = document.getElementById("maxPriceInput").value.trim();
  var sort = document.getElementById("sortFilter").value;

  var params = [];
  if (search) params.push("q=" + encodeURIComponent(search));
  if (category && category !== "all") params.push("category=" + encodeURIComponent(category));
  if (condition && condition !== "all") params.push("condition=" + encodeURIComponent(condition));
  if (status && status !== "all") params.push("status=" + encodeURIComponent(status));
  if (minPrice) params.push("minPrice=" + encodeURIComponent(minPrice));
  if (maxPrice) params.push("maxPrice=" + encodeURIComponent(maxPrice));
  if (sort) params.push("sort=" + encodeURIComponent(sort));

  var url = "/api/listings" + (params.length ? "?" + params.join("&") : "");

  document.getElementById("resultsCount").innerText = "Searching listings...";

  fetch(url)
    .then(function (res) { return res.json(); })
    .then(function (data) {
      currentListings = data;
      renderListings(data);
      updateFilterChips();
    })
    .catch(function (err) {
      console.error("Error fetching listings:", err);
      document.getElementById("resultsCount").innerText = "Failed to load listings.";
      showToast("Failed to connect to backend server.", "error");
    });
}

// Render listings cards into the grid
function renderListings(items) {
  var grid = document.getElementById("listingsGrid");
  var countEl = document.getElementById("resultsCount");
  grid.innerHTML = "";

  countEl.innerHTML = "Showing <strong>" + items.length + "</strong> listing" + (items.length === 1 ? "" : "s");

  if (!items || items.length === 0) {
    grid.innerHTML = "" +
      "<div class='empty-state'>" +
      "  <div class='empty-state-icon'>🔍</div>" +
      "  <h3>No Listings Found</h3>" +
      "  <p>No used items match your search or filter criteria. Try adjusting filters or searching for something else.</p>" +
      "  <button class='btn btn-primary' onclick='resetFilters()'>Reset All Filters</button>" +
      "</div>";
    return;
  }

  items.forEach(function (item) {
    var card = document.createElement("article");
    card.className = "listing-card" + (item.sold ? " is-sold" : "");
    card.id = "listing-" + item.id;

    var inquiriesCount = item.inquiries ? item.inquiries.length : 0;

    // Card Badges
    var statusBadge = item.sold 
      ? "<span class='badge badge-status-sold'>🔒 SOLD</span>"
      : "<span class='badge badge-status-available'>✓ AVAILABLE</span>";

    var condBadge = "<span class='badge badge-condition'>" + escapeHtml(item.condition) + "</span>";
    var catBadge = "<span class='badge badge-category'>" + escapeHtml(item.category) + "</span>";

    // Card Body Content
    var formattedPrice = Number(item.price).toLocaleString("en-IN");

    var cardHtml = "" +
      "<div class='card-top-bar'>" +
      "  <div class='badge-group'>" +
           catBadge +
           condBadge +
      "  </div>" +
         statusBadge +
      "</div>" +

      "<div class='card-body'>" +
      "  <div class='listing-price'><span class='currency'>₹</span>" + formattedPrice + "</div>" +
      "  <h3 class='listing-title'>" + escapeHtml(item.title) + "</h3>" +
      "  <p class='listing-desc'>" + escapeHtml(item.description) + "</p>" +

      "  <div class='seller-info-box'>" +
      "    <div>Seller: <span class='seller-name'>" + escapeHtml(item.sellerName || "Student") + "</span></div>" +
      "    <div>" + escapeHtml(item.sellerContact || "") + "</div>" +
      "  </div>" +

      "  <div class='inquiry-indicator'>" +
      "    <span>💬 <strong>" + inquiriesCount + "</strong> Inquir" + (inquiriesCount === 1 ? "y" : "ies") + "</span>" +
      "    <span>Posted: " + escapeHtml(item.createdAt || "") + "</span>" +
      "  </div>";

    // Requirement 4 & 5: Sold Status Actions & Prevention of New Inquiries
    if (item.sold) {
      cardHtml += "" +
        "<div class='sold-notice-banner'>" +
        "  <span>🚫 Sold Out &bull; New inquiries closed</span>" +
        "</div>" +
        "<div class='card-actions'>" +
        "  <button class='btn btn-outline' onclick='openInquiryModal(" + item.id + ")'>View Questions (" + inquiriesCount + ")</button>" +
        "  <button class='btn btn-sold btn-sm' title='Click to reopen listing if marked sold by mistake' onclick='toggleSoldStatus(" + item.id + ")'>Reopen</button>" +
        "</div>";
    } else {
      cardHtml += "" +
        "<div class='card-actions'>" +
        "  <button class='btn btn-primary' onclick='openInquiryModal(" + item.id + ")'>💬 Ask Question</button>" +
        "  <button class='btn btn-sold' onclick='markAsSold(" + item.id + ")'>✓ Mark Sold</button>" +
        "</div>";
    }

    cardHtml += "</div>"; // end card-body

    card.innerHTML = cardHtml;
    grid.appendChild(card);
  });
}

// Search with Debounce
function handleSearchInput() {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer);
  }
  searchDebounceTimer = setTimeout(function () {
    loadListings();
  }, 250);
}

// Filter change handler
function applyFilters() {
  loadListings();
}

// Quick Category Pill Selector
function selectCategoryPill(category) {
  var pills = document.querySelectorAll(".pill-btn");
  pills.forEach(function (pill) {
    if (pill.getAttribute("data-cat") === category) {
      pill.classList.add("active");
    } else {
      pill.classList.remove("active");
    }
  });

  var catSelect = document.getElementById("categoryFilter");
  catSelect.value = category;
  loadListings();
}

// Reset all search and filter controls
function resetFilters() {
  document.getElementById("searchInput").value = "";
  document.getElementById("categoryFilter").value = "all";
  document.getElementById("conditionFilter").value = "all";
  document.getElementById("statusFilter").value = "all";
  document.getElementById("minPriceInput").value = "";
  document.getElementById("maxPriceInput").value = "";
  document.getElementById("sortFilter").value = "newest";

  var pills = document.querySelectorAll(".pill-btn");
  pills.forEach(function (pill) {
    if (pill.getAttribute("data-cat") === "all") {
      pill.classList.add("active");
    } else {
      pill.classList.remove("active");
    }
  });

  loadListings();
  showToast("Filters reset to default.", "info");
}

// Display Active Filter Chips
function updateFilterChips() {
  var chipsContainer = document.getElementById("activeFilterChips");
  chipsContainer.innerHTML = "";

  var search = document.getElementById("searchInput").value.trim();
  var category = document.getElementById("categoryFilter").value;
  var condition = document.getElementById("conditionFilter").value;
  var status = document.getElementById("statusFilter").value;
  var minPrice = document.getElementById("minPriceInput").value.trim();
  var maxPrice = document.getElementById("maxPriceInput").value.trim();

  if (search) {
    addChip("Keyword: " + search, function () {
      document.getElementById("searchInput").value = "";
      loadListings();
    });
  }

  if (category && category !== "all") {
    addChip("Category: " + category, function () {
      selectCategoryPill("all");
    });
  }

  if (condition && condition !== "all") {
    addChip("Condition: " + condition, function () {
      document.getElementById("conditionFilter").value = "all";
      loadListings();
    });
  }

  if (status && status !== "all") {
    addChip("Status: " + (status === "available" ? "Available Only" : "Sold Only"), function () {
      document.getElementById("statusFilter").value = "all";
      loadListings();
    });
  }

  if (minPrice || maxPrice) {
    var priceText = "Price: ₹" + (minPrice || "0") + " - " + (maxPrice ? "₹" + maxPrice : "Any");
    addChip(priceText, function () {
      document.getElementById("minPriceInput").value = "";
      document.getElementById("maxPriceInput").value = "";
      loadListings();
    });
  }
}

function addChip(text, onRemove) {
  var chipsContainer = document.getElementById("activeFilterChips");
  var chip = document.createElement("span");
  chip.className = "chip";
  chip.innerHTML = escapeHtml(text) + " <span class='chip-close'>&times;</span>";
  chip.querySelector(".chip-close").addEventListener("click", onRemove);
  chipsContainer.appendChild(chip);
}

// Open / Close Modals
function openCreateModal() {
  document.getElementById("createListingForm").reset();
  document.getElementById("createModal").classList.add("active");
}

function closeModal(modalId) {
  document.getElementById(modalId).classList.remove("active");
}

// Close modals when clicking overlay outside card
document.querySelectorAll(".modal-overlay").forEach(function (overlay) {
  overlay.addEventListener("click", function (e) {
    if (e.target === overlay) {
      overlay.classList.remove("active");
    }
  });
});

// Requirement 1: Allow Sellers to Create Listings
function handleCreateListing(e) {
  e.preventDefault();

  var title = document.getElementById("newTitle").value.trim();
  var category = document.getElementById("newCategory").value;
  var condition = document.getElementById("newCondition").value;
  var price = parseFloat(document.getElementById("newPrice").value);
  var sellerName = document.getElementById("newSellerName").value.trim();
  var sellerContact = document.getElementById("newSellerContact").value.trim();
  var description = document.getElementById("newDescription").value.trim();

  if (!title || !category || !condition || isNaN(price) || price < 0 || !description) {
    showToast("Please fill in all required fields properly.", "error");
    return;
  }

  var submitBtn = document.getElementById("createSubmitBtn");
  submitBtn.disabled = true;
  submitBtn.innerText = "Publishing...";

  var payload = {
    title: title,
    category: category,
    condition: condition,
    price: price,
    sellerName: sellerName,
    sellerContact: sellerContact,
    description: description
  };

  fetch("/api/listings", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  })
    .then(function (res) { return res.json(); })
    .then(function (data) {
      submitBtn.disabled = false;
      submitBtn.innerText = "Publish Listing";

      if (data.success) {
        showToast("Listing created successfully!", "success");
        closeModal("createModal");
        loadListings();
        loadStats();
      } else {
        showToast(data.error || "Failed to create listing.", "error");
      }
    })
    .catch(function (err) {
      submitBtn.disabled = false;
      submitBtn.innerText = "Publish Listing";
      showToast("Server error while publishing listing.", "error");
      console.error(err);
    });
}

// Requirement 4: Allow Sellers to Mark a Listing Sold
function markAsSold(listingId) {
  if (!confirm("Are you sure you want to mark this item as SOLD?\n\nOnce marked as sold, the listing remains visible to students, but new inquiries will be blocked.")) {
    return;
  }

  fetch("/api/listings/" + listingId + "/sold", {
    method: "POST"
  })
    .then(function (res) { return res.json(); })
    .then(function (data) {
      if (data.success) {
        showToast("Listing marked as SOLD!", "success");
        loadListings();
        loadStats();
      } else {
        showToast(data.error || "Failed to mark as sold.", "error");
      }
    })
    .catch(function (err) {
      showToast("Error updating listing status.", "error");
      console.error(err);
    });
}

// Optional: Toggle Sold status back to available if marked by mistake
function toggleSoldStatus(listingId) {
  fetch("/api/listings/" + listingId + "/toggle-sold", {
    method: "POST"
  })
    .then(function (res) { return res.json(); })
    .then(function (data) {
      if (data.success) {
        showToast("Listing reopened as Available!", "success");
        loadListings();
        loadStats();
      } else {
        showToast(data.error || "Failed to toggle status.", "error");
      }
    })
    .catch(function (err) {
      showToast("Error updating listing status.", "error");
      console.error(err);
    });
}

// Requirement 3 & 5: Open Inquiries Modal and Enforce Sold Rule
function openInquiryModal(listingId) {
  fetch("/api/listings/" + listingId)
    .then(function (res) { return res.json(); })
    .then(function (item) {
      document.getElementById("inquiryListingId").value = item.id;
      document.getElementById("inquiryModalTitle").innerText = item.title;

      // Render summary box
      var summaryBox = document.getElementById("inquiryItemSummary");
      var statusHtml = item.sold 
        ? "<span class='badge badge-status-sold'>🔒 SOLD</span>" 
        : "<span class='badge badge-status-available'>✓ AVAILABLE</span>";

      summaryBox.innerHTML = "" +
        "<div style='display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem;'>" +
        "  <div>" +
        "    <div style='font-size: 1.1rem; font-weight: 700; color: var(--gray-900);'>" + escapeHtml(item.title) + "</div>" +
        "    <div style='font-size: 0.85rem; color: var(--gray-600); margin: 0.25rem 0;'>" + escapeHtml(item.category) + " &bull; Condition: " + escapeHtml(item.condition) + "</div>" +
        "    <div style='font-size: 0.8rem; color: var(--gray-500);'>Seller: " + escapeHtml(item.sellerName) + " (" + escapeHtml(item.sellerContact) + ")</div>" +
        "  </div>" +
        "  <div style='text-align: right;'>" +
        "    <div style='font-size: 1.35rem; font-weight: 800; color: var(--primary);'>₹" + Number(item.price).toLocaleString("en-IN") + "</div>" +
        "    <div style='margin-top: 0.35rem;'>" + statusHtml + "</div>" +
        "  </div>" +
        "</div>";

      // Render Inquiries Timeline
      var timeline = document.getElementById("inquiriesTimeline");
      timeline.innerHTML = "";
      var inqs = item.inquiries || [];
      document.getElementById("inquiryCountSpan").innerText = inqs.length;

      if (inqs.length === 0) {
        timeline.innerHTML = "<p style='color: var(--gray-500); font-size: 0.85rem; font-style: italic; padding: 0.5rem 0;'>No inquiries yet. Be the first student to ask a question!</p>";
      } else {
        inqs.forEach(function (inq) {
          var bubble = document.createElement("div");
          bubble.className = "inquiry-bubble";
          bubble.innerHTML = "" +
            "<div class='inquiry-bubble-header'>" +
            "  <span class='inquiry-buyer'>👤 " + escapeHtml(inq.buyerName) + (inq.buyerContact ? " <span style='font-weight: normal; color: var(--gray-500);'>(" + escapeHtml(inq.buyerContact) + ")</span>" : "") + "</span>" +
            "  <span class='inquiry-time'>" + escapeHtml(inq.timestamp || "") + "</span>" +
            "</div>" +
            "<div class='inquiry-text'>" + escapeHtml(inq.message) + "</div>";
          timeline.appendChild(bubble);
        });
      }

      // Requirement 5: Check if item is SOLD
      var formContainer = document.getElementById("inquiryFormContainer");
      var soldNotice = document.getElementById("inquirySoldBlockedNotice");
      var submitInqForm = document.getElementById("submitInquiryForm");

      if (item.sold) {
        // Sold item: PREVENT new inquiries!
        formContainer.style.display = "none";
        soldNotice.style.display = "block";
      } else {
        // Available item: Allow inquiries
        formContainer.style.display = "block";
        soldNotice.style.display = "none";
        submitInqForm.reset();
      }

      document.getElementById("inquiryModal").classList.add("active");
    })
    .catch(function (err) {
      showToast("Could not load item details.", "error");
      console.error(err);
    });
}

// Submit Inquiry Handler (with backend confirmation)
function handleSubmitInquiry(e) {
  e.preventDefault();

  var listingId = document.getElementById("inquiryListingId").value;
  var buyerName = document.getElementById("buyerName").value.trim();
  var buyerContact = document.getElementById("buyerContact").value.trim();
  var message = document.getElementById("inquiryMessage").value.trim();

  if (!buyerName || !message) {
    showToast("Please enter your name and question.", "error");
    return;
  }

  var submitBtn = document.getElementById("inquirySubmitBtn");
  submitBtn.disabled = true;
  submitBtn.innerText = "Sending...";

  var payload = {
    buyerName: buyerName,
    buyerContact: buyerContact,
    message: message
  };

  fetch("/api/listings/" + listingId + "/inquiries", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  })
    .then(function (res) {
      return res.json().then(function (data) {
        return { ok: res.ok, status: res.status, data: data };
      });
    })
    .then(function (result) {
      submitBtn.disabled = false;
      submitBtn.innerText = "Send Inquiry";

      if (result.ok && result.data.success) {
        showToast("Inquiry sent to seller!", "success");
        // Reload modal content and listings
        openInquiryModal(listingId);
        loadListings();
        loadStats();
      } else {
        // If rejected (e.g. item sold or validation)
        showToast(result.data.error || "Inquiry could not be submitted.", "error");
      }
    })
    .catch(function (err) {
      submitBtn.disabled = false;
      submitBtn.innerText = "Send Inquiry";
      showToast("Error communicating with server.", "error");
      console.error(err);
    });
}

// Utility: Show Toast Notifications
function showToast(message, type) {
  var container = document.getElementById("toastContainer");
  var toast = document.createElement("div");
  toast.className = "toast toast-" + (type || "info");

  var icon = "ℹ️";
  if (type === "success") icon = "✅";
  if (type === "error") icon = "⚠️";

  toast.innerHTML = "<span>" + icon + "</span> <span>" + escapeHtml(message) + "</span>";
  container.appendChild(toast);

  setTimeout(function () {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(100%)";
    toast.style.transition = "all 0.3s ease";
    setTimeout(function () {
      if (toast.parentNode) {
        toast.parentNode.removeChild(toast);
      }
    }, 300);
  }, 3500);
}

// Utility: HTML escaping
function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
