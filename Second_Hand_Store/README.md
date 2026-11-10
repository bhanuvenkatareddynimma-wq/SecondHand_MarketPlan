# SecondHand MarketPlace

A student-to-student web marketplace for buying and selling used campus items (textbooks, electronics, hostel furniture, lab supplies, bicycles) with an interactive inquiry system.

Built using **Java**, **HTML5**, and **CSS3**.

---

## 🏗️ Architecture & Integration with Your Java Code

The core file [`SecondHandMarketplace.java`](file:///c:/Second_Hand_Store/SecondHandMarketplace.java) directly preserves your original structure and methods while powering the HTML/CSS website:

1. **`class Listing`**:
   - `int id`
   - `String title, description, category, condition`
   - `double price`
   - `boolean sold`
   - `ArrayList<Inquiry> inquiries`
   - `display()` (Console display)
   - `toJson()` (Web JSON serialization)

2. **`public class SecondHandMarketplace`**:
   - `static ArrayList<Listing> items = new ArrayList<Listing>()`
   - `addListing()` & `addListingItem(...)` (Requirement 1: Create listing)
   - `browseListings()` (Requirement 2: Browse listings)
   - `submitInquiry()` & `submitInquiryWeb(...)` (Requirement 3: Submit inquiry)
   - `markAsSold()` & `markItemAsSold(...)` (Requirement 4: Mark listing sold)
   - **Sold Items Policy Enforcement** (Requirement 5: Sold items remain visible, but new inquiries are blocked both on web and in console)
   - `searchAndFilter(...)` (Additional Requirement: Filter by category, condition, and price)
   - `startWebServer(8080)`: Embedded HTTP web server serving `web/index.html`, `web/styles.css`, and `web/app.js`

---

## 🚀 How to Run the Website

### Step 1: In PowerShell or Command Prompt
Run these two commands:
```powershell
cd c:\Second_Hand_Store
javac SecondHandMarketplace.java
java SecondHandMarketplace
```

*(Or simply double-click [`start_server.bat`](file:///c:/Second_Hand_Store/start_server.bat))*

### Step 2: Open in Your Browser
Open any web browser and go to:
👉 **`http://localhost:8080/`**

You can use the website (HTML/CSS) and the console menu simultaneously! Any item added or marked sold via the web immediately reflects in the console, and vice-versa.
