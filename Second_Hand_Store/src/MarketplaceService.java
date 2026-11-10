package server;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketplaceService {
    private final List<Listing> listings = new ArrayList<Listing>();
    private final AtomicInteger nextListingId = new AtomicInteger(1);
    private final AtomicInteger nextInquiryId = new AtomicInteger(1);
    private final File dataFile;

    public MarketplaceService(File dataFile) {
        this.dataFile = dataFile;
        loadOrInitialize();
    }

    private synchronized void loadOrInitialize() {
        if (dataFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                Object parsed = SimpleJson.parse(sb.toString());
                if (parsed instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) parsed;
                    int maxLId = 0;
                    int maxIId = 0;
                    for (Object obj : list) {
                        if (obj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Listing l = Listing.fromMap((Map<String, Object>) obj);
                            listings.add(l);
                            if (l.getId() > maxLId) maxLId = l.getId();
                            for (Inquiry inq : l.getInquiries()) {
                                if (inq.getId() > maxIId) maxIId = inq.getId();
                            }
                        }
                    }
                    nextListingId.set(maxLId + 1);
                    nextInquiryId.set(maxIId + 1);
                    System.out.println("Loaded " + listings.size() + " listings from " + dataFile.getAbsolutePath());
                    return;
                }
            } catch (Exception e) {
                System.err.println("Could not load data file, initializing with sample items: " + e.getMessage());
            }
        }

        // Initialize with realistic student listings
        seedSampleListings();
        saveToFile();
    }

    private synchronized void seedSampleListings() {
        listings.clear();
        nextListingId.set(1);
        nextInquiryId.set(1);

        // Item 1: Textbook
        Listing l1 = new Listing(nextListingId.getAndIncrement(),
                "Higher Engineering Mathematics (B.S. Grewal 44th Edition)",
                "Standard engineering textbook in great condition. Minimal pencil markings, no torn pages. Perfect for semester 1-4 math syllabus.",
                "Books & Study Material", 450.0, "Like New", "Aarav Sharma", "aarav.sharma@campus.edu");
        l1.addInquiry(new Inquiry(nextInquiryId.getAndIncrement(), "Rohan Gupta", "rohan.g@campus.edu",
                "Hi, is this the latest syllabus 44th edition? Can we meet near the library today?"));
        listings.add(l1);

        // Item 2: Electronics / Calculator
        Listing l2 = new Listing(nextListingId.getAndIncrement(),
                "Texas Instruments TI-84 Plus Graphing Calculator",
                "Graphing calculator ideal for engineering, statistics and calculus courses. Works flawlessly with fresh AAA batteries included.",
                "Electronics & Gadgets", 1750.0, "Good", "Priya Nair", "priya.nair@campus.edu");
        l2.addInquiry(new Inquiry(nextInquiryId.getAndIncrement(), "Meera Sen", "meera.sen@campus.edu",
                "Does it come with the USB transfer cable and slip cover?"));
        listings.add(l2);

        // Item 3: Hostel Furniture
        Listing l3 = new Listing(nextListingId.getAndIncrement(),
                "Foldable Wooden Study Table & Ergonomic Mesh Chair",
                "Sturdy wooden study desk with adjustable height and breathable mesh swivel chair. Ideal for hostel rooms or studio apartments.",
                "Hostel & Furniture", 1200.0, "Good", "Kunal Patel", "kunal.patel@campus.edu");
        listings.add(l3);

        // Item 4: Lab Equipment
        Listing l4 = new Listing(nextListingId.getAndIncrement(),
                "White Cotton Lab Coat (Size M) + Chemistry Dissection Kit",
                "Unused lab coat (Size 38/Medium) with college safety specs and dissection tools kit. Clean and washed.",
                "Stationery & Supplies", 350.0, "Brand New", "Sneha Reddy", "sneha.reddy@campus.edu");
        listings.add(l4);

        // Item 5: Sold item to test requirement 4 & 5
        Listing l5 = new Listing(nextListingId.getAndIncrement(),
                "Boat Rockerz 450 Bluetooth Wireless Headphones",
                "Matte black wireless headphones with 15-hour battery backup, deep bass and mic for online lectures. Kept in clean condition.",
                "Electronics & Gadgets", 650.0, "Like New", "Vikram Malhotra", "vikram.m@campus.edu");
        l5.addInquiry(new Inquiry(nextInquiryId.getAndIncrement(), "Aditya Joshi", "aditya.j@campus.edu",
                "Is the aux cable and charging wire available with the box?"));
        l5.setSold(true); // Marked as SOLD to showcase sold status and disabled inquiries!
        listings.add(l5);

        // Item 6: Sports / Bicycle
        Listing l6 = new Listing(nextListingId.getAndIncrement(),
                "Hero Sprint 26T City Bicycle with Cable Lock & Basket",
                "Single-speed campus bicycle in smooth working condition. New brake pads installed last month. Free heavy-duty number combination lock.",
                "Sports & Fitness", 2200.0, "Fair", "Tanmay Joshi", "tanmay.j@campus.edu");
        listings.add(l6);

        // Item 7: Stationery / Mini Drafter
        Listing l7 = new Listing(nextListingId.getAndIncrement(),
                "Omega Engineering Drawing Board (A2) + Mini Drafter with Bag",
                "Complete engineering graphics drafting set. Includes A2 wooden board, smooth rotating mini drafter, sheet clips, and weather-proof carry bag.",
                "Stationery & Supplies", 520.0, "Good", "Divya Verma", "divya.verma@campus.edu");
        listings.add(l7);

        // Item 8: Dorm Lighting
        Listing l8 = new Listing(nextListingId.getAndIncrement(),
                "Rechargeable LED Desk Study Lamp with 3 Color Modes & Pen Stand",
                "Eye-caring LED desk lamp with touch sensor switch, flexible gooseneck, USB rechargeable battery, and built-in mobile/pen stand.",
                "Hostel & Furniture", 320.0, "Brand New", "Rahul Singhania", "rahul.s@campus.edu");
        listings.add(l8);
    }

    public synchronized void saveToFile() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < listings.size(); i++) {
                if (i > 0) sb.append(",\n");
                sb.append("  ").append(listings.get(i).toJson());
            }
            sb.append("\n]");

            FileOutputStream fos = new FileOutputStream(dataFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            osw.write(sb.toString());
            osw.flush();
            osw.close();
        } catch (IOException e) {
            System.err.println("Failed to save data: " + e.getMessage());
        }
    }

    // Requirement 1: Allow sellers to create listings
    public synchronized Listing addListing(String title, String description, String category,
                                          double price, String condition, String sellerName, String sellerContact) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price must be non-negative.");
        }
        if (condition == null || condition.trim().isEmpty()) {
            throw new IllegalArgumentException("Condition is required.");
        }

        Listing listing = new Listing(nextListingId.getAndIncrement(), title.trim(),
                description.trim(), category.trim(), price, condition.trim(), sellerName, sellerContact);
        listings.add(0, listing); // Newest on top
        saveToFile();
        return listing;
    }

    public synchronized Listing getListingById(int id) {
        for (Listing l : listings) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }

    // Requirement 4: Allow sellers to mark a listing sold
    public synchronized boolean markAsSold(int id) {
        Listing listing = getListingById(id);
        if (listing != null) {
            listing.setSold(true);
            saveToFile();
            return true;
        }
        return false;
    }

    // Toggle sold status if needed
    public synchronized boolean toggleSold(int id, boolean sold) {
        Listing listing = getListingById(id);
        if (listing != null) {
            listing.setSold(sold);
            saveToFile();
            return true;
        }
        return false;
    }

    // Requirement 3 & 5: Allow buyers to submit inquiry on available listings,
    // PREVENT new inquiries on sold listings!
    public synchronized InquiryResult submitInquiry(int listingId, String buyerName, String buyerContact, String message) {
        Listing listing = getListingById(listingId);
        if (listing == null) {
            return new InquiryResult(false, "Listing #" + listingId + " not found.", null);
        }

        if (listing.isSold()) {
            // Requirement 5 strictly enforced on the server!
            return new InquiryResult(false, "Inquiry rejected: This item is marked as SOLD. New inquiries are not allowed.", null);
        }

        if (buyerName == null || buyerName.trim().isEmpty()) {
            return new InquiryResult(false, "Buyer name is required.", null);
        }
        if (message == null || message.trim().isEmpty()) {
            return new InquiryResult(false, "Inquiry question/message is required.", null);
        }

        Inquiry inq = new Inquiry(nextInquiryId.getAndIncrement(), buyerName.trim(),
                buyerContact != null ? buyerContact.trim() : "", message.trim());
        boolean added = listing.addInquiry(inq);
        if (added) {
            saveToFile();
            return new InquiryResult(true, "Inquiry submitted successfully!", inq);
        } else {
            return new InquiryResult(false, "This item has been marked as SOLD. Inquiries are closed.", null);
        }
    }

    // Requirement 2 & Additional Requirements: Search and filter listings
    public synchronized List<Listing> searchAndFilter(String query, String category,
                                                      String condition, Double minPrice,
                                                      Double maxPrice, String status, String sort) {
        List<Listing> result = new ArrayList<Listing>();

        for (Listing l : listings) {
            // Status filter: "all", "available", "sold"
            if (status != null && !status.equalsIgnoreCase("all") && !status.isEmpty()) {
                if (status.equalsIgnoreCase("available") && l.isSold()) {
                    continue;
                }
                if (status.equalsIgnoreCase("sold") && !l.isSold()) {
                    continue;
                }
            }

            // Category filter
            if (category != null && !category.equalsIgnoreCase("all") && !category.trim().isEmpty()) {
                if (!l.getCategory().equalsIgnoreCase(category.trim())) {
                    continue;
                }
            }

            // Condition filter
            if (condition != null && !condition.equalsIgnoreCase("all") && !condition.trim().isEmpty()) {
                if (!l.getCondition().equalsIgnoreCase(condition.trim())) {
                    continue;
                }
            }

            // Price filter
            if (minPrice != null && minPrice > 0) {
                if (l.getPrice() < minPrice) {
                    continue;
                }
            }
            if (maxPrice != null && maxPrice > 0) {
                if (l.getPrice() > maxPrice) {
                    continue;
                }
            }

            // Text search in title and description
            if (query != null && !query.trim().isEmpty()) {
                String q = query.trim().toLowerCase();
                String title = l.getTitle().toLowerCase();
                String desc = l.getDescription().toLowerCase();
                if (!title.contains(q) && !desc.contains(q)) {
                    continue;
                }
            }

            result.add(l);
        }

        // Sorting
        if (sort != null) {
            if (sort.equalsIgnoreCase("price_asc")) {
                Collections.sort(result, new Comparator<Listing>() {
                    public int compare(Listing a, Listing b) {
                        return Double.compare(a.getPrice(), b.getPrice());
                    }
                });
            } else if (sort.equalsIgnoreCase("price_desc")) {
                Collections.sort(result, new Comparator<Listing>() {
                    public int compare(Listing a, Listing b) {
                        return Double.compare(b.getPrice(), a.getPrice());
                    }
                });
            } else if (sort.equalsIgnoreCase("title_asc")) {
                Collections.sort(result, new Comparator<Listing>() {
                    public int compare(Listing a, Listing b) {
                        return a.getTitle().compareToIgnoreCase(b.getTitle());
                    }
                });
            } else {
                // Default: newest first (higher ID first)
                Collections.sort(result, new Comparator<Listing>() {
                    public int compare(Listing a, Listing b) {
                        return b.getId() - a.getId();
                    }
                });
            }
        }

        return result;
    }

    public synchronized String getStatsJson() {
        int total = listings.size();
        int active = 0;
        int sold = 0;
        int inquiries = 0;

        for (Listing l : listings) {
            if (l.isSold()) {
                sold++;
            } else {
                active++;
            }
            inquiries += l.getInquiries().size();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalListings\":").append(total).append(",");
        sb.append("\"activeListings\":").append(active).append(",");
        sb.append("\"soldListings\":").append(sold).append(",");
        sb.append("\"totalInquiries\":").append(inquiries);
        sb.append("}");
        return sb.toString();
    }

    public static class InquiryResult {
        public final boolean success;
        public final String message;
        public final Inquiry inquiry;

        public InquiryResult(boolean success, String message, Inquiry inquiry) {
            this.success = success;
            this.message = message;
            this.inquiry = inquiry;
        }
    }
}
