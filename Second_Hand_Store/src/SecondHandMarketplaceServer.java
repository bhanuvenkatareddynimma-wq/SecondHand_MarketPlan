package server;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.*;

public class SecondHandMarketplaceServer {
    private static final int PORT = 8080;
    private static MarketplaceService service;
    private static File webDir;

    public static void main(String[] args) throws IOException {
        int port = PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        File baseDir = new File(".").getAbsoluteFile();
        File dataDir = new File(baseDir, "data");
        File dataFile = new File(dataDir, "listings.json");
        webDir = new File(baseDir, "web");

        service = new MarketplaceService(dataFile);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", new ApiHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(null); // default executor

        System.out.println("==================================================");
        System.out.println("   SECONDHAND MARKETPLACE WEB SERVER STARTED");
        System.out.println("   Local URL: http://localhost:" + port + "/");
        System.out.println("==================================================");

        server.start();
    }

    private static class StaticFileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Remove leading slash
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            File targetFile = new File(webDir, path);

            // Prevent directory traversal
            if (!targetFile.getCanonicalPath().startsWith(webDir.getCanonicalPath())) {
                sendResponse(exchange, 403, "text/plain", "Forbidden");
                return;
            }

            if (!targetFile.exists() || targetFile.isDirectory()) {
                // If asking for a subpage or unknown file, fall back to index.html
                targetFile = new File(webDir, "index.html");
                if (!targetFile.exists()) {
                    sendResponse(exchange, 404, "text/plain", "404 Not Found");
                    return;
                }
            }

            String mime = "text/plain";
            String nameLower = targetFile.getName().toLowerCase();
            if (nameLower.endsWith(".html") || nameLower.endsWith(".htm")) mime = "text/html; charset=UTF-8";
            else if (nameLower.endsWith(".css")) mime = "text/css; charset=UTF-8";
            else if (nameLower.endsWith(".js")) mime = "application/javascript; charset=UTF-8";
            else if (nameLower.endsWith(".json")) mime = "application/json; charset=UTF-8";
            else if (nameLower.endsWith(".png")) mime = "image/png";
            else if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")) mime = "image/jpeg";
            else if (nameLower.endsWith(".svg")) mime = "image/svg+xml";

            byte[] bytes = readFileToByteArray(targetFile);
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    private static class ApiHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            // path format: /api/...
            String subPath = path.substring("/api".length());
            if (subPath.startsWith("/")) {
                subPath = subPath.substring(1);
            }

            try {
                if (subPath.equals("stats") && "GET".equals(method)) {
                    sendJson(exchange, 200, service.getStatsJson());
                    return;
                }

                if (subPath.equals("categories") && "GET".equals(method)) {
                    String json = "[\"Books & Study Material\",\"Electronics & Gadgets\",\"Hostel & Furniture\",\"Stationery & Supplies\",\"Sports & Fitness\",\"Clothing & Fashion\",\"Other\"]";
                    sendJson(exchange, 200, json);
                    return;
                }

                if (subPath.equals("conditions") && "GET".equals(method)) {
                    String json = "[\"Brand New\",\"Like New\",\"Good\",\"Fair\"]";
                    sendJson(exchange, 200, json);
                    return;
                }

                // /api/listings
                if (subPath.equals("listings") || subPath.equals("listings/")) {
                    if ("GET".equals(method)) {
                        handleGetListings(exchange);
                        return;
                    } else if ("POST".equals(method)) {
                        handleCreateListing(exchange);
                        return;
                    }
                }

                // /api/listings/{id}/...
                if (subPath.startsWith("listings/")) {
                    String rest = subPath.substring("listings/".length());
                    int slashIdx = rest.indexOf('/');
                    if (slashIdx == -1) {
                        // /api/listings/{id}
                        int id = Integer.parseInt(rest);
                        if ("GET".equals(method)) {
                            Listing l = service.getListingById(id);
                            if (l != null) {
                                sendJson(exchange, 200, l.toJson());
                            } else {
                                sendJson(exchange, 404, "{\"error\":\"Listing not found\"}");
                            }
                            return;
                        }
                    } else {
                        int id = Integer.parseInt(rest.substring(0, slashIdx));
                        String action = rest.substring(slashIdx + 1);

                        // Requirement 4: Mark listing as sold
                        if ("sold".equals(action) && "POST".equals(method)) {
                            boolean ok = service.markAsSold(id);
                            if (ok) {
                                Listing l = service.getListingById(id);
                                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Listing marked as SOLD\",\"listing\":" + l.toJson() + "}");
                            } else {
                                sendJson(exchange, 404, "{\"success\":false,\"error\":\"Listing not found\"}");
                            }
                            return;
                        }

                        // Toggle sold status
                        if ("toggle-sold".equals(action) && "POST".equals(method)) {
                            Listing l = service.getListingById(id);
                            if (l != null) {
                                service.toggleSold(id, !l.isSold());
                                Listing updated = service.getListingById(id);
                                sendJson(exchange, 200, "{\"success\":true,\"listing\":" + updated.toJson() + "}");
                            } else {
                                sendJson(exchange, 404, "{\"success\":false,\"error\":\"Listing not found\"}");
                            }
                            return;
                        }

                        // Requirement 3 & 5: Inquiries
                        if ("inquiries".equals(action) && "POST".equals(method)) {
                            handleCreateInquiry(exchange, id);
                            return;
                        }
                    }
                }

                sendJson(exchange, 404, "{\"error\":\"Endpoint not found: " + path + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"error\":" + SimpleJson.escape(e.getMessage()) + "}");
            }
        }
    }

    private static void handleGetListings(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
        String q = params.get("q");
        String category = params.get("category");
        String condition = params.get("condition");
        String status = params.get("status");
        String sort = params.get("sort");
        Double minPrice = null;
        Double maxPrice = null;

        if (params.containsKey("minPrice") && !params.get("minPrice").trim().isEmpty()) {
            try {
                minPrice = Double.parseDouble(params.get("minPrice"));
            } catch (NumberFormatException ignored) {}
        }
        if (params.containsKey("maxPrice") && !params.get("maxPrice").trim().isEmpty()) {
            try {
                maxPrice = Double.parseDouble(params.get("maxPrice"));
            } catch (NumberFormatException ignored) {}
        }

        List<Listing> list = service.searchAndFilter(q, category, condition, minPrice, maxPrice, status, sort);

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).toJson());
        }
        sb.append("]");

        sendJson(exchange, 200, sb.toString());
    }

    @SuppressWarnings("unchecked")
    private static void handleCreateListing(HttpExchange exchange) throws IOException {
        String body = readBodyAsString(exchange);
        Object parsed = SimpleJson.parse(body);
        if (!(parsed instanceof Map)) {
            sendJson(exchange, 400, "{\"success\":false,\"error\":\"Invalid JSON body\"}");
            return;
        }

        Map<String, Object> map = (Map<String, Object>) parsed;
        String title = (String) map.get("title");
        String description = (String) map.get("description");
        String category = (String) map.get("category");
        String condition = (String) map.get("condition");
        String sellerName = (String) map.get("sellerName");
        String sellerContact = (String) map.get("sellerContact");

        double price = 0.0;
        if (map.get("price") instanceof Number) {
            price = ((Number) map.get("price")).doubleValue();
        } else if (map.get("price") instanceof String) {
            try {
                price = Double.parseDouble((String) map.get("price"));
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"success\":false,\"error\":\"Price must be a valid number\"}");
                return;
            }
        }

        try {
            Listing created = service.addListing(title, description, category, price, condition, sellerName, sellerContact);
            sendJson(exchange, 201, "{\"success\":true,\"message\":\"Listing created successfully!\",\"listing\":" + created.toJson() + "}");
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"success\":false,\"error\":" + SimpleJson.escape(e.getMessage()) + "}");
        }
    }

    @SuppressWarnings("unchecked")
    private static void handleCreateInquiry(HttpExchange exchange, int listingId) throws IOException {
        String body = readBodyAsString(exchange);
        Object parsed = SimpleJson.parse(body);
        if (!(parsed instanceof Map)) {
            sendJson(exchange, 400, "{\"success\":false,\"error\":\"Invalid JSON body\"}");
            return;
        }

        Map<String, Object> map = (Map<String, Object>) parsed;
        String buyerName = (String) map.get("buyerName");
        String buyerContact = (String) map.get("buyerContact");
        String message = (String) map.get("message");

        // Requirement 3 & 5
        MarketplaceService.InquiryResult result = service.submitInquiry(listingId, buyerName, buyerContact, message);

        if (result.success) {
            sendJson(exchange, 201, "{\"success\":true,\"message\":" + SimpleJson.escape(result.message) + ",\"inquiry\":" + result.inquiry.toJson() + "}");
        } else {
            // Sold item inquiry attempts fail here with HTTP 400
            sendJson(exchange, 400, "{\"success\":false,\"error\":" + SimpleJson.escape(result.message) + "}");
        }
    }

    private static Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> map = new HashMap<String, String>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return map;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String val = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    map.put(key, val);
                } else if (!pair.isEmpty()) {
                    String key = URLDecoder.decode(pair, "UTF-8");
                    map.put(key, "");
                }
            } catch (UnsupportedEncodingException ignored) {}
        }
        return map;
    }

    private static String readBodyAsString(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String mime, String text) throws IOException {
        byte[] bytes = text.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static byte[] readFileToByteArray(File file) throws IOException {
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
}
