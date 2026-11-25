package dsa;

import model.Product;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Merge Sort (O(n log n)) and Binary Search (O(log n)) implementations,
 * plus Linear Search baseline for empirical performance comparison.
 */
public class SearchSortUtils {

    public static List<Product> mergeSort(List<Product> products, String sortBy) {
        if (products.size() <= 1) {
            return new ArrayList<>(products);
        }
        int mid = products.size() / 2;
        List<Product> left = mergeSort(products.subList(0, mid), sortBy);
        List<Product> right = mergeSort(products.subList(mid, products.size()), sortBy);
        return merge(left, right, sortBy);
    }

    private static List<Product> merge(List<Product> left, List<Product> right, String sortBy) {
        List<Product> merged = new ArrayList<>(left.size() + right.size());
        int i = 0, j = 0;

        while (i < left.size() && j < right.size()) {
            if (compareProducts(left.get(i), right.get(j), sortBy) <= 0) {
                merged.add(left.get(i++));
            } else {
                merged.add(right.get(j++));
            }
        }
        while (i < left.size()) merged.add(left.get(i++));
        while (j < right.size()) merged.add(right.get(j++));
        return merged;
    }

    private static int compareProducts(Product a, Product b, String sortBy) {
        switch (sortBy) {
            case "name":
                return a.getName().compareToIgnoreCase(b.getName());
            case "price":
                return Double.compare(a.getPrice(), b.getPrice());
            case "quantity":
                return Integer.compare(a.getQuantity(), b.getQuantity());
            case "sold":
                return Integer.compare(b.getTotalSold(), a.getTotalSold()); // Descending
            case "id":
            default:
                return Integer.compare(a.getId(), b.getId());
        }
    }

    /**
     * Binary Search by Product ID on a list sorted ascending by ID. Time: O(log n).
     */
    public static Product binarySearchById(List<Product> sortedById, int targetId) {
        int low = 0;
        int high = sortedById.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int midId = sortedById.get(mid).getId();

            if (midId == targetId) {
                return sortedById.get(mid);
            } else if (midId < targetId) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return null;
    }

    /**
     * Linear Search baseline by Product ID. Time: O(n).
     */
    public static Product linearSearchById(List<Product> products, int targetId) {
        for (Product p : products) {
            if (p.getId() == targetId) {
                return p;
            }
        }
        return null;
    }
}
