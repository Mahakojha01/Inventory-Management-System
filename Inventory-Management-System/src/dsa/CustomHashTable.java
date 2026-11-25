package dsa;

import model.Product;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Hash Table using Separate Chaining (Linked Lists at each bucket).
 * Provides O(1) average-case insertion, deletion, and lookup by Product ID.
 */
public class CustomHashTable {
    private static class Entry {
        int key;
        Product value;
        Entry next;

        Entry(int key, Product value) {
            this.key = key;
            this.value = value;
        }
    }

    private Entry[] buckets;
    private int capacity;
    private int size;

    public CustomHashTable(int capacity) {
        this.capacity = capacity;
        this.buckets = new Entry[capacity];
        this.size = 0;
    }

    private int hash(int key) {
        // Multiplicative hashing to reduce clustering
        long mixed = Integer.toUnsignedLong(key) * 2654435761L;
        return (int) (Math.abs(mixed) % capacity);
    }

    public void put(Product product) {
        int index = hash(product.getId());
        Entry head = buckets[index];

        Entry current = head;
        while (current != null) {
            if (current.key == product.getId()) {
                current.value = product;
                return;
            }
            current = current.next;
        }

        Entry newNode = new Entry(product.getId(), product);
        newNode.next = head;
        buckets[index] = newNode;
        size++;
    }

    public Product get(int id) {
        int index = hash(id);
        Entry current = buckets[index];
        while (current != null) {
            if (current.key == id) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public boolean remove(int id) {
        int index = hash(id);
        Entry current = buckets[index];
        Entry prev = null;

        while (current != null) {
            if (current.key == id) {
                if (prev == null) {
                    buckets[index] = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
                return true;
            }
            prev = current;
            current = current.next;
        }
        return false;
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            Entry current = buckets[i];
            while (current != null) {
                list.add(current.value);
                current = current.next;
            }
        }
        return list;
    }

    public int getSize() {
        return size;
    }
}
