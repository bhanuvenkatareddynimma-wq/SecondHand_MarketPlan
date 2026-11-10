import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

// ==========================================
// 1. INQUIRY MODEL (Requirement 3 & 5)
// ==========================================
class Inquiry {
    int id;
    String buyerName;
    String buyerContact;
    String message;
    String timestamp;

    Inquiry(int id, String buyerName, String buyerContact, String message) {
        this.id = id;
        this.buyerName = (buyerName != null && !buyerName.trim().isEmpty()) ? buyerName.trim() : "Student";
        this.buyerContact = (buyerContact != null) ? buyerContact.trim() : "";
        this.message = (message != null) ? message.trim() : "";
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    Inquiry(int id, String buyerName, String buyerContact, String message, String timestamp) {
        this.id = id;
        this.buyerName = buyerName;
        this.buyerContact = buyerContact;
        this.message = message;
        this.timestamp = timestamp;
    }

    String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"buyerName\":" + SecondHandMarketplace.escapeJson(buyerName) + ","
                + "\"buyerContact\":" + SecondHandMarketplace.escapeJson(buyerContact) + ","
                + "\"message\":" + SecondHandMarketplace.escapeJson(message) + ","
                + "\"timestamp\":" + SecondHandMarketplace.escapeJson(timestamp)
                + "}";
    }
}

// ==========================================
// 2. LISTING MODEL (Requirement 1, 2, 4, 5)
// ==========================================
class Listing {
    int id;
    String title, description, category, condition;
    double price;
    boolean sold;
    String sellerName;
    String sellerContact;
    String createdAt;
    ArrayList<Inquiry> inquiries = new ArrayList<Inquiry>();

    Listing(int id, String title, String description,
            String category, double price, String condition) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.condition = condition;
        this.sold = false;
        this.sellerName = "Student Seller";
        this.sellerContact = "campus@student.edu";
        this.createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    Listing(int id, String title, String description,
            String category, double price, String condition,
            String sellerName, String sellerContact) {
        this(id, title, description, category, price, condition);
        if (sellerName != null && !sellerName.trim().isEmpty()) {
            this.sellerName = sellerName.trim();
        }
        if (sellerContact != null && !sellerContact.trim().isEmpty()) {
            this.sellerContact = sellerContact.trim();
        }
    }

    // Display for Console CLI
    void display() {
        System.out.println("\n-------------------------------------------");
        System.out.println("Listing ID: " + id);
        System.out.println("Title: " + title);
        System.out.println("Description: " + description);
        System.out.println("Category: " + category);
        System.out.println("Price: Rs." + price);
        System.out.println("Condition: " + condition);
        System.out.println("Status: " + (sold ? "SOLD" : "AVAILABLE"));
        if (!inquiries.isEmpty()) {
            System.out.println("Inquiries (" + inquiries.size() + "):");
            for (Inquiry inq : inquiries) {
                System.out.println("  - [" + inq.timestamp + "] " + inq.buyerName + ": " + inq.message);
            }
        }
        System.out.println("-------------------------------------------");
    }

    // Serializes Listing to JSON for the Web Front-End (HTML/CSS)
    synchronized String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(id).append(",");
        sb.append("\"title\":").append(SecondHandMarketplace.escapeJson(title)).append(",");
        sb.append("\"description\":").append(SecondHandMarketplace.escapeJson(description)).append(",");
        sb.append("\"category\":").append(SecondHandMarketplace.escapeJson(category)).append(",");
        sb.append("\"price\":").append(price).append(",");
        sb.append("\"condition\":").append(SecondHandMarketplace.escapeJson(condition)).append(",");
        sb.append("\"sellerName\":").append(SecondHandMarketplace.escapeJson(sellerName)).append(",");
        sb.append("\"sellerContact\":").append(SecondHandMarketplace.escapeJson(sellerContact)).append(",");
        sb.append("\"sold\":").append(sold).append(",");
        sb.append("\"createdAt\":").append(SecondHandMarketplace.escapeJson(createdAt)).append(",");
        sb.append("\"inquiries\":[");
        for (int i = 0; i < inquiries.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(inquiries.get(i).toJson());
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }
}

// ==========================================
// 3. MAIN SECONDHAND MARKETPLACE CLASS
// ==========================================
public class SecondHandMarketplace {

    static ArrayList<Listing> items = new ArrayList<Listing>();
    static Scanner sc = new Scanner(System.in);
    static int nextId = 1;
    static int nextInquiryId = 1;
    static final int PORT = 8080;
    static final File DATA_FILE = new File("data", "listings.json");
    static final File WEB_DIR = new File("web");

    // =============================================================
    // REQUIREMENT 1: CREATE LISTING (Console & Programmatic)
    // =============================================================
    static void addListing() {
        System.out.print("Enter title: ");
        String title = sc.nextLine();

        System.out.print("Enter description: ");
        String description = sc.nextLine();

        System.out.print("Enter category: ");
        String category = sc.nextLine();

        System.out.print("Enter price: ");
        double price = 0.0;
        try {
            price = Double.parseDouble(sc.nextLine());
        } catch (Exception e) {
            System.out.println("Invalid price. Setting to 0.");
        }

        System.out.print("Enter condition: ");
        String condition = sc.nextLine();

        System.out.print("Enter seller name (optional): ");
        String sellerName = sc.nextLine();

        System.out.print("Enter seller contact (optional): ");
        String sellerContact = sc.nextLine();

        Listing l = addListingItem(title, description, category, price, condition, sellerName, sellerContact);
        System.out.println("Listing #" + l.id + " added successfully!");
    }

    static synchronized Listing addListingItem(String title, String description, String category,
                                               double price, String condition, String sellerName, String sellerContact) {
        Listing item = new Listing(nextId++, title, description, category, price, condition, sellerName, sellerContact);
        items.add(0, item); // Show newest first
        saveData();
        return item;
    }

    // =============================================================
    // REQUIREMENT 2: BROWSE LISTINGS (Console)
    // =============================================================
    static void browseListings() {
        if (items.isEmpty()) {
            System.out.println("No listings available.");
            return;
        }

        System.out.println("\n========== ALL MARKETPLACE LISTINGS (" + items.size() + ") ==========");
        for (Listing item : items) {
            item.display();
        }
    }

    // =============================================================
    // REQUIREMENT 3 & 5: SUBMIT INQUIRY WITH SOLD VALIDATION
    // =============================================================
    static void submitInquiry() {
        System.out.print("Enter Listing ID: ");
        int id = 0;
        try {
            id = Integer.parseInt(sc.nextLine());
        } catch (Exception e) {
            System.out.println("Invalid ID.");
            return;
        }

        for (Listing item : items) {
            if (item.id == id) {
                // Requirement 5: Keep sold listings visible, but prevent new inquiries on them!
                if (item.sold) {
                    System.out.println("Sorry! This item is already sold. New inquiries are closed.");
                } else {
                    System.out.print("Enter your name: ");
                    String name = sc.nextLine();

                    System.out.print("Enter your contact: ");
                    String contact = sc.nextLine();

                    System.out.print("Enter your inquiry: ");
                    String inquiryText = sc.nextLine();

                    Inquiry inq = new Inquiry(nextInquiryId++, name, contact, inquiryText);
                    item.inquiries.add(inq);
                    saveData();

                    System.out.println("Inquiry submitted successfully!");
                    System.out.println("Inquiry: " + inquiryText);
                }
                return;
            }
        }
        System.out.println("Listing not found.");
    }

    // Programmatic Inquiry Submission (used by Web API)
    static synchronized boolean submitInquiryWeb(int id, String buyerName, String buyerContact, String message, StringBuilder outError) {
        for (Listing item : items) {
            if (item.id == id) {
                if (item.sold) {
                    outError.append("This item has been marked as SOLD. New inquiries are not allowed.");
                    return false;
                }
                Inquiry inq = new Inquiry(nextInquiryId++, buyerName, buyerContact, message);
                item.inquiries.add(inq);
                saveData();
                return true;
            }
        }
        outError.append("Listing not found.");
        return false;
    }

    // =============================================================
    // REQUIREMENT 4: MARK AN ITEM AS SOLD
    // =============================================================
    static void markAsSold() {
        System.out.print("Enter Listing ID: ");
        int id = 0;
        try {
            id = Integer.parseInt(sc.nextLine());
        } catch (Exception e) {
            System.out.println("Invalid ID.");
            return;
        }

        if (markItemAsSold(id)) {
            System.out.println("Item #" + id + " marked as SOLD.");
        } else {
            System.out.println("Listing not found.");
        }
    }

    static synchronized boolean markItemAsSold(int id) {
        for (Listing item : items) {
            if (item.id == id) {
                item.sold = true;
                saveData();
                return true;
            }
        }
        return false;
    }

    static synchronized boolean toggleSoldStatus(int id) {
        for (Listing item : items) {
            if (item.id == id) {
                item.sold = !item.sold;
                saveData();
                return true;
            }
        }
        return false;
    }

    // =============================================================
    // ADDITIONAL REQUIREMENT: SEARCH & FILTER (Category, Condition, Price)
    // =============================================================
    static synchronized List<Listing> searchAndFilter(String query, String category,
                                                      String condition, Double minPrice,
                                                      Double maxPrice, String status, String sort) {
        List<Listing> filtered = new ArrayList<Listing>();

        for (Listing item : items) {
            // Status filter: all, available, sold
            if (status != null && !status.equalsIgnoreCase("all") && !status.isEmpty()) {
                if (status.equalsIgnoreCase("available") && item.sold) continue;
                if (status.equalsIgnoreCase("sold") && !item.sold) continue;
            }

            // Category filter
            if (category != null && !category.equalsIgnoreCase("all") && !category.trim().isEmpty()) {
                if (!item.category.equalsIgnoreCase(category.trim())) continue;
            }

            // Condition filter
            if (condition != null && !condition.equalsIgnoreCase("all") && !condition.trim().isEmpty()) {
                if (!item.condition.equalsIgnoreCase(condition.trim())) continue;
            }

            // Price filter (minPrice & maxPrice)
            if (minPrice != null && minPrice > 0) {
                if (item.price < minPrice) continue;
            }
            if (maxPrice != null && maxPrice > 0) {
                if (item.price > maxPrice) continue;
            }

            // Keyword search (title & description)
            if (query != null && !query.trim().isEmpty()) {
                String q = query.trim().toLowerCase();
                if (!item.title.toLowerCase().contains(q) && !item.description.toLowerCase().contains(q)) {
                    continue;
                }
            }

            filtered.add(item);
        }

        // Sorting
        if (sort != null) {
            if (sort.equalsIgnoreCase("price_asc")) {
                Collections.sort(filtered, new Comparator<Listing>() {
                    public int compare(Listing a, Listing b) {
                        return Double.compare(a.price, b.price);
                    }
                });
            } else if (sort.equalsIgnoreCase("price_desc")) {
                Collections.sort(filtered, new Comparator<Listing>() {
                    public int compare(Listing a, Listing b) {
                        return Double.compare(b.price, a.price);
                    }
                });
            } else if (sort.equalsIgnoreCase("title_asc")) {
                Collections.sort(filtered, new Comparator<Listing>() {
                    public int compare(Listing a, Listing b) {
                        return a.title.compareToIgnoreCase(b.title);
                    }
                });
            } else {
                // Newest first by default
                Collections.sort(filtered, new Comparator<Listing>() {
                    public int compare(Listing a, Listing b) {
                        return b.id - a.id;
                    }
                });
            }
        }

        return filtered;
    }

    // =============================================================
    // DATA PERSISTENCE & SAMPLE SEEDING
    // =============================================================
    static synchronized void loadOrSeedData() {
        if (DATA_FILE.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DATA_FILE), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                Object parsed = parseJson(sb.toString());
                if (parsed instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) parsed;
                    int maxLId = 0;
                    int maxIId = 0;
                    for (Object obj : list) {
                        if (obj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) obj;
                            int id = ((Number) map.get("id")).intValue();
                            String title = (String) map.get("title");
                            String desc = (String) map.get("description");
                            String cat = (String) map.get("category");
                            double pr = ((Number) map.get("price")).doubleValue();
                            String cond = (String) map.get("condition");
                            String sName = (String) map.get("sellerName");
                            String sCont = (String) map.get("sellerContact");
                            boolean sold = (Boolean) map.get("sold");
                            String crAt = (String) map.get("createdAt");

                            Listing l = new Listing(id, title, desc, cat, pr, cond, sName, sCont);
                            l.sold = sold;
                            if (crAt != null) l.createdAt = crAt;

                            @SuppressWarnings("unchecked")
                            List<Object> rawInqs = (List<Object>) map.get("inquiries");
                            if (rawInqs != null) {
                                for (Object inqObj : rawInqs) {
                                    if (inqObj instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> iMap = (Map<String, Object>) inqObj;
                                        int inqId = ((Number) iMap.get("id")).intValue();
                                        String bName = (String) iMap.get("buyerName");
                                        String bCont = (String) iMap.get("buyerContact");
                                        String msg = (String) iMap.get("message");
                                        String ts = (String) iMap.get("timestamp");
                                        l.inquiries.add(new Inquiry(inqId, bName, bCont, msg, ts));
                                        if (inqId > maxIId) maxIId = inqId;
                                    }
                                }
                            }
                            items.add(l);
                            if (id > maxLId) maxLId = id;
                        }
                    }
                    nextId = maxLId + 1;
                    nextInquiryId = maxIId + 1;
                    System.out.println("Loaded " + items.size() + " listings from " + DATA_FILE.getPath());
                    return;
                }
            } catch (Exception e) {
                System.err.println("Notice: Initializing fresh sample listings: " + e.getMessage());
            }
        }

        // Seed realistic student items
        items.clear();
        nextId = 1;
        nextInquiryId = 1;

        Listing l1 = new Listing(nextId++, "Higher Engineering Mathematics (B.S. Grewal 44th Edition)",
                "Standard engineering textbook in great condition. Minimal pencil markings, no torn pages. Perfect for semester 1-4 math.",
                "Books & Study Material", 450.0, "Like New", "Aarav Sharma", "aarav.s@campus.edu");
        l1.inquiries.add(new Inquiry(nextInquiryId++, "Rohan Gupta", "rohan.g@campus.edu",
                "Hi, is this the 44th edition? Can we meet near the library?"));
        items.add(l1);

        Listing l2 = new Listing(nextId++, "Texas Instruments TI-84 Plus Graphing Calculator",
                "Graphing calculator ideal for engineering, statistics and calculus courses. Works flawlessly with fresh AAA batteries.",
                "Electronics & Gadgets", 1750.0, "Good", "Priya Nair", "priya.n@campus.edu");
        l2.inquiries.add(new Inquiry(nextInquiryId++, "Meera Sen", "meera.s@campus.edu",
                "Does it come with the USB transfer cable and slip cover?"));
        items.add(l2);

        Listing l3 = new Listing(nextId++, "Foldable Wooden Study Table & Ergonomic Mesh Chair",
                "Sturdy wooden study desk with adjustable height and breathable mesh swivel chair. Ideal for hostel rooms.",
                "Hostel & Furniture", 1200.0, "Good", "Kunal Patel", "kunal.p@campus.edu");
        items.add(l3);

        Listing l4 = new Listing(nextId++, "White Cotton Lab Coat (Size M) + Chemistry Safety Glasses",
                "Unused lab coat (Size 38/Medium) with college safety specs and dissection tools kit. Clean and washed.",
                "Stationery & Supplies", 350.0, "Brand New", "Sneha Reddy", "sneha.r@campus.edu");
        items.add(l4);

        Listing l5 = new Listing(nextId++, "Boat Rockerz 450 Bluetooth Wireless Headphones",
                "Matte black wireless headphones with 15-hour battery backup, deep bass and mic for lectures. Kept in clean condition.",
                "Electronics & Gadgets", 650.0, "Like New", "Vikram Malhotra", "vikram.m@campus.edu");
        l5.inquiries.add(new Inquiry(nextInquiryId++, "Aditya Joshi", "aditya.j@campus.edu",
                "Is the aux cable and charging wire available with the box?"));
        l5.sold = true; // Seeded as SOLD to immediately demonstrate requirement 4 & 5!
        items.add(l5);

        Listing l6 = new Listing(nextId++, "Hero Sprint 26T City Bicycle with Cable Lock & Basket",
                "Single-speed campus bicycle in smooth working condition. New brake pads installed last month. Free heavy-duty lock.",
                "Sports & Fitness", 2200.0, "Fair", "Tanmay Joshi", "tanmay.j@campus.edu");
        items.add(l6);

        saveData();
    }

    static synchronized void saveData() {
        try {
            if (!DATA_FILE.getParentFile().exists()) {
                DATA_FILE.getParentFile().mkdirs();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) sb.append(",\n");
                sb.append("  ").append(items.get(i).toJson());
            }
            sb.append("\n]");

            FileOutputStream fos = new FileOutputStream(DATA_FILE);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            osw.write(sb.toString());
            osw.flush();
            osw.close();
        } catch (IOException e) {
            System.err.println("Could not save data: " + e.getMessage());
        }
    }

    // =============================================================
    // 4. EMBEDDED HTTP WEB SERVER (Serving HTML, CSS, JavaScript)
    // =============================================================
    static void startWebServer(int port) {
        try {
            HttpServer webServer = HttpServer.create(new InetSocketAddress(port), 0);

            // API Endpoint Handler
            webServer.createContext("/api/", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

                    String method = exchange.getRequestMethod().toUpperCase();
                    if ("OPTIONS".equals(method)) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }

                    String path = exchange.getRequestURI().getPath();
                    String subPath = path.substring("/api".length());
                    if (subPath.startsWith("/")) subPath = subPath.substring(1);

                    try {
                        // GET /api/stats
                        if ("stats".equals(subPath) && "GET".equals(method)) {
                            int total = items.size();
                            int active = 0, soldCount = 0, totalInqs = 0;
                            for (Listing l : items) {
                                if (l.sold) soldCount++; else active++;
                                totalInqs += l.inquiries.size();
                            }
                            String json = "{\"totalListings\":" + total + ",\"activeListings\":" + active
                                    + ",\"soldListings\":" + soldCount + ",\"totalInquiries\":" + totalInqs + "}";
                            sendJson(exchange, 200, json);
                            return;
                        }

                        // GET /api/listings (with Search and Filter)
                        if (("listings".equals(subPath) || "listings/".equals(subPath)) && "GET".equals(method)) {
                            Map<String, String> qp = parseQueryParams(exchange.getRequestURI().getRawQuery());
                            String q = qp.get("q");
                            String cat = qp.get("category");
                            String cond = qp.get("condition");
                            String stat = qp.get("status");
                            String srt = qp.get("sort");
                            Double minP = qp.containsKey("minPrice") ? parseDouble(qp.get("minPrice")) : null;
                            Double maxP = qp.containsKey("maxPrice") ? parseDouble(qp.get("maxPrice")) : null;

                            List<Listing> filtered = searchAndFilter(q, cat, cond, minP, maxP, stat, srt);
                            StringBuilder sb = new StringBuilder();
                            sb.append("[");
                            for (int i = 0; i < filtered.size(); i++) {
                                if (i > 0) sb.append(",");
                                sb.append(filtered.get(i).toJson());
                            }
                            sb.append("]");
                            sendJson(exchange, 200, sb.toString());
                            return;
                        }

                        // POST /api/listings (Create Listing)
                        if (("listings".equals(subPath) || "listings/".equals(subPath)) && "POST".equals(method)) {
                            String body = readBody(exchange);
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) parseJson(body);
                            String title = (String) map.get("title");
                            String desc = (String) map.get("description");
                            String cat = (String) map.get("category");
                            String cond = (String) map.get("condition");
                            double price = ((Number) map.get("price")).doubleValue();
                            String sName = (String) map.get("sellerName");
                            String sCont = (String) map.get("sellerContact");

                            Listing created = addListingItem(title, desc, cat, price, cond, sName, sCont);
                            sendJson(exchange, 201, "{\"success\":true,\"listing\":" + created.toJson() + "}");
                            return;
                        }

                        // Specific Listing actions: /api/listings/{id}/...
                        if (subPath.startsWith("listings/")) {
                            String rest = subPath.substring("listings/".length());
                            int slash = rest.indexOf('/');
                            if (slash == -1) {
                                int id = Integer.parseInt(rest);
                                for (Listing l : items) {
                                    if (l.id == id) {
                                        sendJson(exchange, 200, l.toJson());
                                        return;
                                    }
                                }
                                sendJson(exchange, 404, "{\"error\":\"Not found\"}");
                                return;
                            } else {
                                int id = Integer.parseInt(rest.substring(0, slash));
                                String action = rest.substring(slash + 1);

                                // Requirement 4: Mark Listing Sold
                                if ("sold".equals(action) && "POST".equals(method)) {
                                    boolean ok = markItemAsSold(id);
                                    if (ok) {
                                        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Item marked as SOLD\"}");
                                    } else {
                                        sendJson(exchange, 404, "{\"error\":\"Listing not found\"}");
                                    }
                                    return;
                                }

                                if ("toggle-sold".equals(action) && "POST".equals(method)) {
                                    boolean ok = toggleSoldStatus(id);
                                    sendJson(exchange, ok ? 200 : 404, "{\"success\":" + ok + "}");
                                    return;
                                }

                                // Requirement 3 & 5: Inquiries (prevent if sold!)
                                if ("inquiries".equals(action) && "POST".equals(method)) {
                                    String body = readBody(exchange);
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> map = (Map<String, Object>) parseJson(body);
                                    String bName = (String) map.get("buyerName");
                                    String bCont = (String) map.get("buyerContact");
                                    String msg = (String) map.get("message");

                                    StringBuilder err = new StringBuilder();
                                    boolean success = submitInquiryWeb(id, bName, bCont, msg, err);

                                    if (success) {
                                        sendJson(exchange, 201, "{\"success\":true,\"message\":\"Inquiry submitted successfully!\"}");
                                    } else {
                                        // Requirement 5: Return HTTP 400 Bad Request if item is sold
                                        sendJson(exchange, 400, "{\"success\":false,\"error\":" + escapeJson(err.toString()) + "}");
                                    }
                                    return;
                                }
                            }
                        }

                        sendJson(exchange, 404, "{\"error\":\"Unknown API endpoint\"}");
                    } catch (Exception e) {
                        sendJson(exchange, 500, "{\"error\":" + escapeJson(e.getMessage()) + "}");
                    }
                }
            });

            // Static File Handler (HTML, CSS, JS)
            webServer.createContext("/", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String path = exchange.getRequestURI().getPath();
                    if (path == null || path.equals("/") || path.isEmpty()) {
                        path = "/index.html";
                    }
                    if (path.startsWith("/")) path = path.substring(1);

                    File f = new File(WEB_DIR, path);
                    if (!f.exists() || f.isDirectory()) {
                        f = new File(WEB_DIR, "index.html");
                    }

                    if (!f.exists()) {
                        byte[] err = "404 Not Found".getBytes("UTF-8");
                        exchange.sendResponseHeaders(404, err.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(err);
                        os.close();
                        return;
                    }

                    String mime = "text/html; charset=UTF-8";
                    if (f.getName().endsWith(".css")) mime = "text/css; charset=UTF-8";
                    else if (f.getName().endsWith(".js")) mime = "application/javascript; charset=UTF-8";
                    else if (f.getName().endsWith(".json")) mime = "application/json; charset=UTF-8";

                    byte[] bytes = readFile(f);
                    exchange.getResponseHeaders().set("Content-Type", mime);
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                }
            });

            webServer.start();
            System.out.println("===============================================================");
            System.out.println("   SECONDHAND MARKETPLACE - WEB SERVER ACTIVE!");
            System.out.println("   Open Website: http://localhost:" + port + "/");
            System.out.println("===============================================================\n");
        } catch (Exception e) {
            System.err.println("Notice: Could not start web server on port " + port + ": " + e.getMessage());
        }
    }

    // Helper: read file bytes
    private static byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = fis.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        fis.close();
        return baos.toByteArray();
    }

    private static void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] b = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, b.length);
        OutputStream os = exchange.getResponseBody();
        os.write(b);
        os.close();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        br.close();
        return sb.toString();
    }

    private static Map<String, String> parseQueryParams(String raw) {
        Map<String, String> map = new HashMap<String, String>();
        if (raw == null || raw.isEmpty()) return map;
        String[] parts = raw.split("&");
        for (String p : parts) {
            int eq = p.indexOf('=');
            try {
                if (eq > 0) {
                    map.put(URLDecoder.decode(p.substring(0, eq), "UTF-8"), URLDecoder.decode(p.substring(eq + 1), "UTF-8"));
                } else {
                    map.put(URLDecoder.decode(p, "UTF-8"), "");
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    private static Double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    // =============================================================
    // 5. JSON HELPER (Compatible with Java 7)
    // =============================================================
    public static String escapeJson(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static Object parseJson(String text) {
        if (text == null) return null;
        return new JsonMinParser(text.trim()).parseVal();
    }

    private static class JsonMinParser {
        private final String s;
        private int p = 0;
        JsonMinParser(String s) { this.s = s; }

        private void skip() {
            while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++;
        }
        private char peek() { skip(); return p < s.length() ? s.charAt(p) : '\0'; }
        private char next() { skip(); return p < s.length() ? s.charAt(p++) : '\0'; }

        Object parseVal() {
            char c = peek();
            if (c == '{') return parseObj();
            if (c == '[') return parseArr();
            if (c == '"') return parseStr();
            if (c == 't' || c == 'f') return parseBool();
            if (c == 'n') { p += 4; return null; }
            if (c == '-' || Character.isDigit(c)) return parseNum();
            return null;
        }

        Map<String, Object> parseObj() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            next(); // '{'
            if (peek() == '}') { next(); return map; }
            while (true) {
                String k = parseStr();
                next(); // ':'
                Object v = parseVal();
                map.put(k, v);
                char c = next();
                if (c == '}') break;
                if (c != ',') break;
            }
            return map;
        }

        List<Object> parseArr() {
            List<Object> list = new ArrayList<Object>();
            next(); // '['
            if (peek() == ']') { next(); return list; }
            while (true) {
                list.add(parseVal());
                char c = next();
                if (c == ']') break;
                if (c != ',') break;
            }
            return list;
        }

        String parseStr() {
            next(); // '"'
            StringBuilder sb = new StringBuilder();
            while (p < s.length()) {
                char c = s.charAt(p++);
                if (c == '"') return sb.toString();
                if (c == '\\' && p < s.length()) {
                    char esc = s.charAt(p++);
                    if (esc == '"') sb.append('"');
                    else if (esc == '\\') sb.append('\\');
                    else if (esc == 'n') sb.append('\n');
                    else if (esc == 'r') sb.append('\r');
                    else if (esc == 't') sb.append('\t');
                    else sb.append(esc);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean parseBool() {
            if (s.startsWith("true", p)) { p += 4; return Boolean.TRUE; }
            if (s.startsWith("false", p)) { p += 5; return Boolean.FALSE; }
            return Boolean.FALSE;
        }

        Number parseNum() {
            int st = p;
            if (peek() == '-') p++;
            while (p < s.length() && (Character.isDigit(s.charAt(p)) || s.charAt(p) == '.')) p++;
            String sub = s.substring(st, p);
            if (sub.contains(".")) return Double.parseDouble(sub);
            return Long.parseLong(sub);
        }
    }

    // =============================================================
    // 6. MAIN MENU (Console & Web Integration)
    // =============================================================
    public static void main(String[] args) {
        // Load data or initialize sample student listings
        loadOrSeedData();

        // Start Web Server on Port 8080 (Serves HTML, CSS, JavaScript & REST API)
        startWebServer(PORT);

        while (true) {
            System.out.println("\n===== SECOND-HAND MARKETPLACE =====");
            System.out.println("Website URL: http://localhost:" + PORT + "/");
            System.out.println("1. Add Listing");
            System.out.println("2. Browse Listings");
            System.out.println("3. Submit Inquiry");
            System.out.println("4. Mark Item as Sold");
            System.out.println("5. Exit");

            System.out.print("Enter your choice: ");
            int choice = -1;
            try {
                choice = Integer.parseInt(sc.nextLine());
            } catch (Exception e) {
                System.out.println("Invalid input! Please enter a number between 1 and 5.");
                continue;
            }

            switch (choice) {
                case 1:
                    addListing();
                    break;

                case 2:
                    browseListings();
                    break;

                case 3:
                    submitInquiry();
                    break;

                case 4:
                    markAsSold();
                    break;

                case 5:
                    System.out.println("Thank you for using SecondHand MarketPlace!");
                    sc.close();
                    System.exit(0);
                    return;

                default:
                    System.out.println("Invalid choice! Please select 1 to 5.");
            }
        }
    }
}