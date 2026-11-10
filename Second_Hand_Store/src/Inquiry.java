package server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Inquiry {
    private int id;
    private String buyerName;
    private String buyerContact;
    private String message;
    private String timestamp;

    public Inquiry() {
    }

    public Inquiry(int id, String buyerName, String buyerContact, String message) {
        this.id = id;
        this.buyerName = buyerName;
        this.buyerContact = buyerContact;
        this.message = message;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    public Inquiry(int id, String buyerName, String buyerContact, String message, String timestamp) {
        this.id = id;
        this.buyerName = buyerName;
        this.buyerContact = buyerContact;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getBuyerName() { return buyerName; }
    public String getBuyerContact() { return buyerContact; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(id).append(",");
        sb.append("\"buyerName\":").append(SimpleJson.escape(buyerName)).append(",");
        sb.append("\"buyerContact\":").append(SimpleJson.escape(buyerContact)).append(",");
        sb.append("\"message\":").append(SimpleJson.escape(message)).append(",");
        sb.append("\"timestamp\":").append(SimpleJson.escape(timestamp));
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static Inquiry fromMap(Map<String, Object> map) {
        int id = map.get("id") != null ? ((Number) map.get("id")).intValue() : 0;
        String buyerName = (String) map.get("buyerName");
        String buyerContact = (String) map.get("buyerContact");
        String message = (String) map.get("message");
        String timestamp = (String) map.get("timestamp");
        return new Inquiry(id, buyerName, buyerContact, message, timestamp != null ? timestamp : "");
    }
}
