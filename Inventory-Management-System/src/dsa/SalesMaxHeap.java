package dsa;

import model.Product;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Max-Heap ordered by Product totalSold count.
 * Enables O(1) lookup of top-selling product and O(k log n) top-K ranking.
 */
public class SalesMaxHeap {
    private final List<Product> heap;

    public SalesMaxHeap() {
        this.heap = new ArrayList<>();
    }

    public void buildFromProducts(List<Product> products) {
        heap.clear();
        heap.addAll(products);
        for (int i = (heap.size() / 2) - 1; i >= 0; i--) {
            heapifyDown(i);
        }
    }

    public Product extractMax() {
        if (heap.isEmpty()) return null;
        Product max = heap.get(0);
        Product last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            heapifyDown(0);
        }
        return max;
    }

    public List<Product> getTopBestSellers(int k) {
        SalesMaxHeap temp = new SalesMaxHeap();
        temp.buildFromProducts(this.heap);

        List<Product> top = new ArrayList<>();
        int count = 0;
        while (count < k && !temp.heap.isEmpty()) {
            top.add(temp.extractMax());
            count++;
        }
        return top;
    }

    private void heapifyDown(int index) {
        int size = heap.size();
        while (true) {
            int largest = index;
            int left = 2 * index + 1;
            int right = 2 * index + 2;

            if (left < size && heap.get(left).getTotalSold() > heap.get(largest).getTotalSold()) {
                largest = left;
            }
            if (right < size && heap.get(right).getTotalSold() > heap.get(largest).getTotalSold()) {
                largest = right;
            }
            if (largest != index) {
                swap(index, largest);
                index = largest;
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
