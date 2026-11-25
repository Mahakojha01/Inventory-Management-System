package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import db.DatabaseManager;
import dsa.*;
import model.Product;
import model.TransactionRecord;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Integrates Frontend HTTP Requests, Database Persistence, and all 5 Custom DSA Structures.
 */
public class InventoryController implements HttpHandler {
    private final DatabaseManager db;
    private final CustomHashTable hashTable;
    private final AVLTree avlTree;
    private final StockMinHeap minHeap;
    private final SalesMaxHeap maxHeap;

    public InventoryController() {
        this.db = new DatabaseManager();
        this.hashTable = new CustomHashTable(128);
        this.avlTree = new AVLTree();
        this.minHeap = new StockMinHeap();
        this.maxHeap = new SalesMaxHeap();
        syncDataStructuresFromDatabase();
    }

    private synchronized void syncDataStructuresFromDatabase() {
        List<Product> products = db.loadAllProducts();
        for (Product p : products) {
            hashTable.put(p);
            avlTree.insert(p);
        }
        minHeap.buildFromProducts(products);
        maxHeap.buildFromProducts(products);
    }

    private synchronized void rebuildHeaps() {
        List<Product> all = hashTable.getAllProducts();
        minHeap.buildFromProducts(all);
        maxHeap.buildFromProducts(all);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String method = exchange.getRequestMethod();

        if (path.startsWith("/api/")) {
            handleApi(exchange, path, method, uri.getQuery());
        } else {
            serveStaticFile(exchange, path);
        }
    }

    private void handleApi(HttpExchange exchange, String path, String method, String rawQuery) throws IOException {
        Map<String, String> params = parseQueryParams(rawQuery);
        try {
            if (path.equals("/api/dashboard") && method.equals("GET")) {
                String sortBy = params.getOrDefault("sortBy", "id");
                List<Product> sorted = SearchSortUtils.mergeSort(hashTable.getAllProducts(), sortBy);
                List<Product> lowStock = minHeap.getLowStockAlerts();
                List<Product> bestSellers = maxHeap.getTopBestSellers(5);
                List<TransactionRecord> history = db.loadHistory();

                StringBuilder json = new StringBuilder("{");
                json.append(""dbMode":"").append(db.isMysqlConnected() ? "MySQL JDBC Connected" : "Local Storage Mode (Ready for MySQL)").append("",");
                json.append(""avlHeight":").append(avlTree.getTreeHeight()).append(",");
                json.append(""products":").append(productsToJson(sorted)).append(",");
                json.append(""lowStockAlerts":").append(productsToJson(lowStock)).append(",");
                json.append(""bestSellers":").append(productsToJson(bestSellers)).append(",");
                json.append(""history":").append(historyToJson(history));
                json.append("}");
                sendJson(exchange, 200, json.toString());

            } else if (path.equals("/api/search") && method.equals("GET")) {
                String type = params.getOrDefault("type", "id");
                String q = params.getOrDefault("q", "").trim();
                List<Product> results = new ArrayList<>();
                String dsaUsed;

                if (q.isEmpty()) {
                    results = SearchSortUtils.mergeSort(hashTable.getAllProducts(), "id");
                    dsaUsed = "Merge Sort (All Records)";
                } else if (type.equals("id")) {
                    int id = Integer.parseInt(q);
                    Product found = hashTable.get(id);
                    if (found != null) results.add(found);
                    dsaUsed = "Custom Hash Table O(1) Lookup";
                } else {
                    Product exact = avlTree.searchByName(q);
                    if (exact != null) {
                        results.add(exact);
                    } else {
                        results = avlTree.searchByPrefix(q);
                    }
                    dsaUsed = "Self-Balancing AVL Tree O(log n) Search";
                }

                String resp = String.format("{"dsaUsed":"%s","results":%s}",
                        Product.escapeJson(dsaUsed), productsToJson(results));
                sendJson(exchange, 200, resp);

            } else if (path.equals("/api/product") && method.equals("POST")) {
                Map<String, String> body = parseFormBody(exchange);
                int id = Integer.parseInt(body.get("id"));
                String name = body.get("name").trim();
                String category = body.get("category").trim();
                double price = Double.parseDouble(body.get("price"));
                int quantity = Integer.parseInt(body.get("quantity"));
                int threshold = Integer.parseInt(body.getOrDefault("minThreshold", "10"));

                Product existing = hashTable.get(id);
                if (existing != null) {
                    avlTree.delete(existing.getName());
                    existing.setName(name);
                    existing.setCategory(category);
                    existing.setPrice(price);
                    existing.setQuantity(quantity);
                    existing.setMinThreshold(threshold);
                    hashTable.put(existing);
                    avlTree.insert(existing);
                    rebuildHeaps();
                    db.upsertProduct(existing, hashTable.getAllProducts());
                    db.recordTransaction(id, name, "UPDATE", quantity, quantity);
                } else {
                    Product created = new Product(id, name, category, price, quantity, threshold, 0);
                    hashTable.put(created);
                    avlTree.insert(created);
                    rebuildHeaps();
                    db.upsertProduct(created, hashTable.getAllProducts());
                    db.recordTransaction(id, name, "ADD", quantity, quantity);
                }
                sendJson(exchange, 200, "{"status":"ok"}");

            } else if (path.equals("/api/product") && method.equals("DELETE")) {
                int id = Integer.parseInt(params.get("id"));
                Product existing = hashTable.get(id);
                if (existing != null) {
                    hashTable.remove(id);
                    avlTree.delete(existing.getName());
                    rebuildHeaps();
                    db.deleteProduct(id, hashTable.getAllProducts());
                    db.recordTransaction(id, existing.getName(), "DELETE", existing.getQuantity(), 0);
                }
                sendJson(exchange, 200, "{"status":"deleted"}");

            } else if (path.equals("/api/stock") && method.equals("POST")) {
                Map<String, String> body = parseFormBody(exchange);
                int id = Integer.parseInt(body.get("id"));
                String action = body.get("action"); // IN or OUT
                int delta = Integer.parseInt(body.get("amount"));

                Product p = hashTable.get(id);
                if (p == null) {
                    sendJson(exchange, 404, "{"error":"Product ID not found in Hash Table"}");
                    return;
                }
                if ("IN".equalsIgnoreCase(action)) {
                    p.setQuantity(p.getQuantity() + delta);
                    db.recordTransaction(id, p.getName(), "STOCK_IN", delta, p.getQuantity());
                } else {
                    if (p.getQuantity() < delta) {
                        sendJson(exchange, 400, "{"error":"Insufficient stock available"}");
                        return;
                    }
                    p.setQuantity(p.getQuantity() - delta);
                    p.setTotalSold(p.getTotalSold() + delta);
                    db.recordTransaction(id, p.getName(), "STOCK_OUT", delta, p.getQuantity());
                }
                rebuildHeaps();
                db.upsertProduct(p, hashTable.getAllProducts());
                sendJson(exchange, 200, "{"status":"updated"}");

            } else if (path.equals("/api/benchmark") && method.equals("GET")) {
                int datasetSize = Integer.parseInt(params.getOrDefault("size", "10000"));
                String json = runSearchBenchmark(datasetSize);
                sendJson(exchange, 200, json);
            } else {
                sendJson(exchange, 404, "{"error":"Endpoint not found"}");
            }
        } catch (Exception e) {
            sendJson(exchange, 400, "{"error":"" + Product.escapeJson(e.getMessage()) + ""}");
        }
    }

    private String runSearchBenchmark(int n) {
        CustomHashTable benchHash = new CustomHashTable(Math.max(1024, n * 2));
        AVLTree benchAvl = new AVLTree();
        List<Product> list = new ArrayList<>(n);

        for (int i = 1; i <= n; i++) {
            Product p = new Product(i, "Item-" + i, "Benchmark", 100.0, 50, 10, i % 100);
            benchHash.put(p);
            benchAvl.insert(p);
            list.add(p);
        }

        List<Product> sortedForBinary = SearchSortUtils.mergeSort(list, "id");
        int targetId = (int) (n * 0.85);
        String targetName = "Item-" + targetId;
        int iterations = 1000;

        // 1. Custom Hash Table O(1)
        long t1 = System.nanoTime();
        for (int i = 0; i < iterations; i++) benchHash.get(targetId);
        long hashTimeNs = (System.nanoTime() - t1) / iterations;

        // 2. AVL Tree O(log n)
        long t2 = System.nanoTime();
        for (int i = 0; i < iterations; i++) benchAvl.searchByName(targetName);
        long avlTimeNs = (System.nanoTime() - t2) / iterations;

        // 3. Binary Search O(log n)
        long t3 = System.nanoTime();
        for (int i = 0; i < iterations; i++) SearchSortUtils.binarySearchById(sortedForBinary, targetId);
        long binaryTimeNs = (System.nanoTime() - t3) / iterations;

        // 4. Linear Search O(n)
        long t4 = System.nanoTime();
        for (int i = 0; i < iterations; i++) SearchSortUtils.linearSearchById(list, targetId);
        long linearTimeNs = (System.nanoTime() - t4) / iterations;

        return String.format(
            "{"datasetSize":%d,"hashNs":%d,"avlNs":%d,"binaryNs":%d,"linearNs":%d,"avlHeight":%d}",
            n, hashTimeNs, avlTimeNs, binaryTimeNs, linearTimeNs, benchAvl.getTreeHeight()
        );
    }

    private String productsToJson(List<Product> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    private String historyToJson(List<TransactionRecord> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    private void serveStaticFile(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/")) path = "/index.html";
        Path filePath = Paths.get("web" + path).normalize();
        if (!filePath.startsWith(Paths.get("web")) || !Files.exists(filePath)) {
            byte[] notFound = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, notFound.length);
            exchange.getResponseBody().write(notFound);
            exchange.close();
            return;
        }
        String contentType = "text/plain";
        if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
        else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
        else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";

        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private Map<String, String> parseFormBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseQueryParams(body);
    }
}
