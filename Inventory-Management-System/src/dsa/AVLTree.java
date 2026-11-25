package dsa;

import model.Product;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-Balancing AVL Binary Search Tree keyed by Product Name (case-insensitive).
 * Guarantees O(log n) search, insertion, deletion, and alphabetical in-order traversal.
 */
public class AVLTree {
    private static class Node {
        Product product;
        Node left;
        Node right;
        int height;

        Node(Product product) {
            this.product = product;
            this.height = 1;
        }
    }

    private Node root;

    private int height(Node n) {
        return (n == null) ? 0 : n.height;
    }

    private int getBalance(Node n) {
        return (n == null) ? 0 : height(n.left) - height(n.right);
    }

    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;

        x.right = y;
        y.left = T2;

        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        return x;
    }

    private Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;

        y.left = x;
        x.right = T2;

        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        return y;
    }

    public void insert(Product product) {
        root = insertRec(root, product);
    }

    private Node insertRec(Node node, Product product) {
        if (node == null) {
            return new Node(product);
        }

        int cmp = product.getName().compareToIgnoreCase(node.product.getName());
        if (cmp < 0) {
            node.left = insertRec(node.left, product);
        } else if (cmp > 0) {
            node.right = insertRec(node.right, product);
        } else {
            node.product = product;
            return node;
        }

        node.height = 1 + Math.max(height(node.left), height(node.right));
        int balance = getBalance(node);

        // Left Left Case
        if (balance > 1 && product.getName().compareToIgnoreCase(node.left.product.getName()) < 0) {
            return rightRotate(node);
        }
        // Right Right Case
        if (balance < -1 && product.getName().compareToIgnoreCase(node.right.product.getName()) > 0) {
            return leftRotate(node);
        }
        // Left Right Case
        if (balance > 1 && product.getName().compareToIgnoreCase(node.left.product.getName()) > 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        // Right Left Case
        if (balance < -1 && product.getName().compareToIgnoreCase(node.right.product.getName()) < 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    public Product searchByName(String name) {
        Node current = root;
        while (current != null) {
            int cmp = name.compareToIgnoreCase(current.product.getName());
            if (cmp == 0) {
                return current.product;
            } else if (cmp < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return null;
    }

    public List<Product> searchByPrefix(String query) {
        List<Product> results = new ArrayList<>();
        String lower = query.toLowerCase();
        collectMatching(root, lower, results);
        return results;
    }

    private void collectMatching(Node node, String lowerQuery, List<Product> results) {
        if (node == null) return;
        collectMatching(node.left, lowerQuery, results);
        if (node.product.getName().toLowerCase().contains(lowerQuery)) {
            results.add(node.product);
        }
        collectMatching(node.right, lowerQuery, results);
    }

    public void delete(String name) {
        root = deleteRec(root, name);
    }

    private Node minValueNode(Node node) {
        Node current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }

    private Node deleteRec(Node root, String name) {
        if (root == null) return null;

        int cmp = name.compareToIgnoreCase(root.product.getName());
        if (cmp < 0) {
            root.left = deleteRec(root.left, name);
        } else if (cmp > 0) {
            root.right = deleteRec(root.right, name);
        } else {
            if ((root.left == null) || (root.right == null)) {
                Node temp = (root.left != null) ? root.left : root.right;
                root = temp;
            } else {
                Node temp = minValueNode(root.right);
                root.product = temp.product;
                root.right = deleteRec(root.right, temp.product.getName());
            }
        }

        if (root == null) return null;

        root.height = Math.max(height(root.left), height(root.right)) + 1;
        int balance = getBalance(root);

        if (balance > 1 && getBalance(root.left) >= 0) return rightRotate(root);
        if (balance > 1 && getBalance(root.left) < 0) {
            root.left = leftRotate(root.left);
            return rightRotate(root);
        }
        if (balance < -1 && getBalance(root.right) <= 0) return leftRotate(root);
        if (balance < -1 && getBalance(root.right) > 0) {
            root.right = rightRotate(root.right);
            return leftRotate(root);
        }
        return root;
    }

    public List<Product> inOrderTraversal() {
        List<Product> sorted = new ArrayList<>();
        inOrderRec(root, sorted);
        return sorted;
    }

    private void inOrderRec(Node node, List<Product> list) {
        if (node != null) {
            inOrderRec(node.left, list);
            list.add(node.product);
            inOrderRec(node.right, list);
        }
    }

    public int getTreeHeight() {
        return height(root);
    }
}
