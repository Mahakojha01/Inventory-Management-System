package dsa;

import model.Product;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Min-Heap ordered by Product Stock Quantity.
 * Enables O(1) access to the lowest-stock item and O(log n) low-stock alert extraction.
 */
public class StockMinHeap {
    private final List<Product> heap;

    public StockMinHeap() {
        this.heap = new ArrayList<>();
    }

    public void buildFromProducts(List<Product> products) {
        heap.clear();
        heap.addAll(products);
        for (int i = (heap.size() / 2) - 1; i >= 0; i--) {
            heapifyDown(i);
        }
    }

    public void insert(Product p) {
        heap.add(p);
        heapifyUp(heap.size() - 1);
    }

    public Product peekMin() {
        return heap.isEmpty() ? null : heap.get(0);
    }

    public Product extractMin() {
        if (heap.isEmpty()) return null;
        Product min = heap.get(0);
        Product last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            heapifyDown(0);
        }
        return min;
    }

    public List<Product> getLowStockAlerts() {
        // Clone heap to extract items in ascending order of stock without mutating live heap
        StockMinHeap temp = new StockMinHeap();
        temp.buildFromProducts(this.heap);

        List<Product> alerts = new ArrayList<>();
        while (temp.peekMin() != null) {
            Product p = temp.extractMin();
            if (p.isLowStock()) {
                alerts.add(p);
            }
        }
        return alerts;
    }

    private void heapifyUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (heap.get(index).getQuantity() < heap.get(parent).getQuantity()) {
                swap(index, parent);
                index = parent;
            } else {
                break;
            }
        }
    }

    private void heapifyDown(int index) {
        int size = heap.size();
        while (true) {
            int smallest = index;
            int left = 2 * index + 1;
            int right = 2 * index + 2;

            if (left < size && heap.get(left).getQuantity() < heap.get(smallest).getQuantity()) {
                smallest = left;
            }
            if (right < size && heap.get(right).getQuantity() < heap.get(smallest).getQuantity()) {
                smallest = right;
            }
            if (smallest != index) {
                swap(index, smallest);
                index = smallest;
            } else {
                break;
            }
        }
    }

    private void swap(int i, int j) {
        Product temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
}
