package db;

import model.Product;
import model.TransactionRecord;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles MySQL JDBC connectivity with automatic fallback to local file persistence
 * so the project runs cleanly on any student laptop with or without MySQL running.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory_dsa_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    private Connection connection;
    private boolean mysqlConnected = false;
    private static final String LOCAL_PRODUCTS_FILE = "data/products.tsv";
    private static final String LOCAL_HISTORY_FILE = "data/history.tsv";
    private int localTxCounter = 1;

    public DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            mysqlConnected = true;
            System.out.println("[DB] Connected to MySQL database successfully.");
        } catch (Exception e) {
            mysqlConnected = false;
            System.out.println("[DB] MySQL driver/server not active (" + e.getMessage() + ").");
            System.out.println("[DB] Running in Local Storage Fallback Mode (data/products.tsv).");
            initLocalFallbackData();
        }
    }

    public boolean isMysqlConnected() {
        return mysqlConnected;
    }

    private void initLocalFallbackData() {
        try {
            Files.createDirectories(Paths.get("data"));
            File prodFile = new File(LOCAL_PRODUCTS_FILE);
            if (!prodFile.exists()) {
                List<Product> defaults = new ArrayList<>();
                defaults.add(new Product(101, "Wireless Mechanical Keyboard", "Peripherals", 3499.00, 28, 10, 64));
                defaults.add(new Product(102, "USB-C Docking Station", "Accessories", 4299.00, 7, 10, 52));
                defaults.add(new Product(103, "27-Inch IPS Monitor", "Displays", 16500.00, 14, 5, 39));
                defaults.add(new Product(104, "Ergonomic Wireless Mouse", "Peripherals", 1499.00, 4, 12, 95));
                defaults.add(new Product(105, "NVMe 1TB SSD", "Storage", 6899.00, 22, 8, 78));
                defaults.add(new Product(106, "Noise Cancelling Headset", "Audio", 5499.00, 6, 8, 44));
                defaults.add(new Product(107, "Laptop Cooling Pad", "Accessories", 1199.00, 35, 10, 31));
                defaults.add(new Product(108, "1080p Webcam Pro", "Peripherals", 2799.00, 9, 10, 48));
                saveAllProductsLocal(defaults);
            }
            File histFile = new File(LOCAL_HISTORY_FILE);
            if (!histFile.exists()) {
                histFile.createNewFile();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public List<Product> loadAllProducts() {
        List<Product> list = new ArrayList<>();
        if (mysqlConnected) {
            String sql = "SELECT product_id, name, category, price, quantity, min_threshold, total_sold FROM products";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getInt("min_threshold"),
                        rs.getInt("total_sold")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(LOCAL_PRODUCTS_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split("\t");
                    if (parts.length >= 7) {
                        list.add(new Product(
                            Integer.parseInt(parts[0]),
                            parts[1],
                            parts[2],
                            Double.parseDouble(parts[3]),
                            Integer.parseInt(parts[4]),
                            Integer.parseInt(parts[5]),
                            Integer.parseInt(parts[6])
                        ));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public void upsertProduct(Product p, List<Product> currentSnapshot) {
        if (mysqlConnected) {
            String sql = "INSERT INTO products (product_id, name, category, price, quantity, min_threshold, total_sold) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                         "name=VALUES(name), category=VALUES(category), price=VALUES(price), " +
                         "quantity=VALUES(quantity), min_threshold=VALUES(min_threshold), total_sold=VALUES(total_sold)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, p.getId());
                ps.setString(2, p.getName());
                ps.setString(3, p.getCategory());
                ps.setDouble(4, p.getPrice());
                ps.setInt(5, p.getQuantity());
                ps.setInt(6, p.getMinThreshold());
                ps.setInt(7, p.getTotalSold());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            saveAllProductsLocal(currentSnapshot);
        }
    }

    public void deleteProduct(int productId, List<Product> currentSnapshot) {
        if (mysqlConnected) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM products WHERE product_id = ?")) {
                ps.setInt(1, productId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            saveAllProductsLocal(currentSnapshot);
        }
    }

    private void saveAllProductsLocal(List<Product> products) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOCAL_PRODUCTS_FILE))) {
            for (Product p : products) {
                pw.printf("%d	%s	%s	%.2f	%d	%d	%d%n",
                    p.getId(), p.getName().replace("	", " "), p.getCategory().replace("	", " "),
                    p.getPrice(), p.getQuantity(), p.getMinThreshold(), p.getTotalSold());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recordTransaction(int productId, String productName, String actionType, int qtyChanged, int resultingStock) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (mysqlConnected) {
            String sql = "INSERT INTO stock_history (product_id, product_name, action_type, quantity_changed, resulting_stock, tx_timestamp) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, productId);
                ps.setString(2, productName);
                ps.setString(3, actionType);
                ps.setInt(4, qtyChanged);
                ps.setInt(5, resultingStock);
                ps.setString(6, ts);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try (PrintWriter pw = new PrintWriter(new FileWriter(LOCAL_HISTORY_FILE, true))) {
                pw.printf("%d	%d	%s	%s	%d	%d	%s%n",
                    localTxCounter++, productId, productName.replace("	", " "),
                    actionType, qtyChanged, resultingStock, ts);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<TransactionRecord> loadHistory() {
        List<TransactionRecord> records = new ArrayList<>();
        if (mysqlConnected) {
            String sql = "SELECT tx_id, product_id, product_name, action_type, quantity_changed, resulting_stock, tx_timestamp FROM stock_history ORDER BY tx_id DESC LIMIT 50";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    records.add(new TransactionRecord(
                        rs.getInt("tx_id"),
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getString("action_type"),
                        rs.getInt("quantity_changed"),
                        rs.getInt("resulting_stock"),
                        rs.getString("tx_timestamp")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            List<TransactionRecord> all = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(LOCAL_HISTORY_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] p = line.split("\t");
                    if (p.length >= 7) {
                        all.add(new TransactionRecord(
                            Integer.parseInt(p[0]), Integer.parseInt(p[1]), p[2],
                            p[3], Integer.parseInt(p[4]), Integer.parseInt(p[5]), p[6]
                        ));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = all.size() - 1; i >= 0 && records.size() < 50; i--) {
                records.add(all.get(i));
            }
        }
        return records;
    }
}
