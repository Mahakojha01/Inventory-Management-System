package model;

public class TransactionRecord {
    private int txId;
    private int productId;
    private String productName;
    private String actionType;
    private int quantityChanged;
    private int resultingStock;
    private String timestamp;

    public TransactionRecord(int txId, int productId, String productName, String actionType,
                             int quantityChanged, int resultingStock, String timestamp) {
        this.txId = txId;
        this.productId = productId;
        this.productName = productName;
        this.actionType = actionType;
        this.quantityChanged = quantityChanged;
        this.resultingStock = resultingStock;
        this.timestamp = timestamp;
    }

    public int getTxId() { return txId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getActionType() { return actionType; }
    public int getQuantityChanged() { return quantityChanged; }
    public int getResultingStock() { return resultingStock; }
    public String getTimestamp() { return timestamp; }

    public String toJson() {
        return String.format(
            "{"txId":%d,"productId":%d,"productName":"%s","actionType":"%s","quantityChanged":%d,"resultingStock":%d,"timestamp":"%s"}",
            txId, productId, Product.escapeJson(productName), Product.escapeJson(actionType),
            quantityChanged, resultingStock, Product.escapeJson(timestamp)
        );
    }
}
