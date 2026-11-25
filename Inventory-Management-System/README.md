# Inventory Management System Using Advanced Data Structures

An end-to-end Java full-stack web application built to manage product inventory, stock-in/stock-out operations, low-stock alerts, sales rankings, transaction history, and live Data Structure search benchmarking.

## Tech Stack
- **Backend:** Pure Java (`com.sun.net.httpserver.HttpServer`, Java 17+)
- **DSA Engine (Custom Implementations in Java):**
  - **Custom Hash Table** (Separate Chaining with polynomial rolling hash) — $O(1)$ average lookup by Product ID
  - **AVL Tree** (Self-Balancing Binary Search Tree) — $O(\log n)$ lookup by Product Name and sorted traversal
  - **Min-Heap** — $O(1)$ peek / $O(\log n)$ update for Low-Stock Alerts
  - **Max-Heap** — $O(1)$ peek / $O(\log n)$ update for Best-Selling Products ranking
  - **Merge Sort & Binary Search** — $O(n \log n)$ custom sorting and $O(\log n)$ search on sorted arrays
- **Frontend:** HTML5, CSS3, Vanilla JavaScript (Responsive Dashboard + Live Benchmark Visualizer)
- **Database:** MySQL (`mysql-connector-j` via JDBC) with automatic local persistence fallback so the app runs out-of-the-box even before MySQL is configured.

## Folder Structure
```text
Inventory-Management-System-DSA/
├── src/
│   ├── Main.java                     # Application entry point & HTTP server starter
│   ├── model/
│   │   ├── Product.java              # Product entity
│   │   └── TransactionRecord.java    # Stock-In / Stock-Out / Sale history entity
│   ├── dsa/
│   │   ├── CustomHashTable.java      # Separate chaining Hash Table (Search by ID)
│   │   ├── AVLTree.java              # Self-balancing BST (Search by Name & In-Order)
│   │   ├── StockMinHeap.java         # Min-Heap for Low-Stock Alerts
│   │   ├── SalesMaxHeap.java         # Max-Heap for Best-Selling Products
│   │   └── SearchSortUtils.java      # Merge Sort, Binary Search & Linear Search
│   ├── db/
│   │   └── DatabaseManager.java      # JDBC MySQL persistence + local file sync
│   └── server/
│       └── InventoryController.java  # REST API & DSA synchronization layer
├── web/
│   ├── index.html                    # Web UI
│   ├── style.css                     # Dashboard styling
│   └── app.js                        # Frontend API integration
├── sql/
│   └── schema.sql                    # MySQL database & table creation script
├── lib/                              # Place mysql-connector-j-8.x.x.jar here for MySQL
├── run.bat                           # One-click compile & run script for Windows
├── run.sh                            # One-click compile & run script for Linux/macOS
└── README.md
```
