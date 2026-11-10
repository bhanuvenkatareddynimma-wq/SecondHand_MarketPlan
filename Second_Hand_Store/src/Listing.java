package server;

import java.text.SimpleDateFormat;
import java.util.*;

public class Listing {
    private int id;
    private String title;
    private String description;
    private String category;
    private double price;
    private String condition;
    private String sellerName;
    private String sellerContact;
    private boolean sold;
    private String createdAt;
    private List<Inquiry> inquiries = new ArrayList<Inquiry>();

    public Listing() {
    }

    public Listing(int id, String title, String description, String category,
                   double price, String condition, String sellerName, String sellerContact) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.condition = condition;
        this.sellerName = (sellerName != null && !sellerName.trim().isEmpty()) ? sellerName : "Student Seller";
        this.sellerContact = (sellerContact != null && !sellerContact.trim().isEmpty()) ? sellerContact : "campus-seller@student.edu";
        this.sold = false;
        this.createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    public synchronized int getId() { return id; }
    public synchronized String getTitle() { return title; }
    public synchronized String getDescription() { return description; }
    public synchronized String getCategory() { return category; }
    public synchronized double getPrice() { return price; }
    public synchronized String getCondition() { return condition; }
    public synchronized String getSellerName() { return sellerName; }
    public synchronized String getSellerContact() { return sellerContact; }
    public synchronized boolean isSold() { return sold; }
    public synchronized String getCreatedAt() { return createdAt; }
    public synchronized List<Inquiry> getInquiries() { return new ArrayList<Inquiry>(inquiries); }

    public synchronized void setSold(boolean sold) {
        this.sold = sold;
    }

    /**
     * Requirement 5: Keep sold listings visible, but prevent new inquiries on them.
     * Returns true if inquiry was successfully added, false if item is sold.
     */
    public synchronized boolean addInquiry(Inquiry inquiry) {
        if (this.sold) {
            return false; // Prevent new inquiries on sold items
        }
        this.inquiries.add(inquiry);
        return true;
    }

    public synchronized String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(id).append(",");
        sb.append("\"title\":").append(SimpleJson.escape(title)).append(",");
        sb.append("\"description\":").append(SimpleJson.escape(description)).append(",");
        sb.append("\"category\":").append(SimpleJson.escape(category)).append(",");
        sb.append("\"price\":").append(price).append(",");
        sb.append("\"condition\":").append(SimpleJson.escape(condition)).append(",");
        sb.append("\"sellerName\":").append(SimpleJson.escape(sellerName)).append(",");
        sb.append("\"sellerContact\":").append(SimpleJson.escape(sellerContact)).append(",");
        sb.append("\"sold\":").append(sold).append(",");
        sb.append("\"createdAt\":").append(SimpleJson.escape(createdAt)).append(",");
        sb.append("\"inquiries\":[");
        for (int i = 0; i < inquiries.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(inquiries.get(i).toJson());
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static Listing fromMap(Map<String, Object> map) {
        Listing listing = new Listing();
        listing.id = map.get("id") != null ? ((Number) map.get("id")).intValue() : 0;
        listing.title = (String) map.get("title");
        listing.description = (String) map.get("description");
        listing.category = (String) map.get("category");
        listing.price = map.get("price") != null ? ((Number) map.get("price")).doubleValue() : 0.0;
        listing.condition = (String) map.get("condition");
        listing.sellerName = (String) map.get("sellerName");
        listing.sellerContact = (String) map.get("sellerContact");
        listing.sold = map.get("sold") != null ? ((Boolean) map.get("sold")).booleanValue() : false;
        listing.createdAt = (String) map.get("createdAt");
        
        List<Object> rawInquiries = (List<Object>) map.get("inquiries");
        if (rawInquiries != null) {
            for (Object obj : rawInquiries) {
                if (obj instanceof Map) {
                    listing.inquiries.add(Inquiry.fromMap((Map<String, Object>) obj));
                }
            }
        }
        return listing;
    }
}
