package com.excelsql.engine.index;

import java.io.Serializable;
import java.util.*;

/**
 * @Description: B+树索引实现
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:42
 */

public class BPlusTreeIndex implements Serializable {

    private static final int ORDER = 4; // B+ tree order
    private Node root;

    public BPlusTreeIndex() {
        this.root = new LeafNode();
    }

    public void insert(Object key, Integer rowNumber) {
        root.insert(key, rowNumber);
        if (root.isOverflow()) {
            Node newRoot = new InternalNode();
            newRoot.keys.add(root.split());
            newRoot.children.add(root);
            root = newRoot;
        }
    }

    public List<Integer> search(Object key) {
        return root.search(key);
    }

    public void delete(Object key, Integer rowNumber) {
        root.delete(key, rowNumber);
    }

    // Abstract Node class
    abstract class Node implements Serializable {
        List<Comparable> keys;

        Node() {
            this.keys = new ArrayList<>();
        }

        abstract void insert(Object key, Integer rowNumber);
        abstract List<Integer> search(Object key);
        abstract void delete(Object key, Integer rowNumber);
        abstract boolean isOverflow();
        abstract Comparable split();
    }

    // Internal Node implementation
    class InternalNode extends Node {
        List<Node> children;

        InternalNode() {
            super();
            this.children = new ArrayList<>();
        }

        @Override
        void insert(Object key, Integer rowNumber) {
            int index = Collections.binarySearch(keys, (Comparable) key);
            if (index < 0) {
                index = -index - 1;
            }

            Node child = children.get(index);
            child.insert(key, rowNumber);

            if (child.isOverflow()) {
                Comparable newKey = child.split();
                keys.add(index, newKey);
                // Handle split
            }
        }

        @Override
        List<Integer> search(Object key) {
            int index = Collections.binarySearch(keys, (Comparable) key);
            if (index < 0) {
                index = -index - 1;
            }
            return children.get(index).search(key);
        }

        @Override
        void delete(Object key, Integer rowNumber) {
            int index = Collections.binarySearch(keys, (Comparable) key);
            if (index < 0) {
                index = -index - 1;
            }
            children.get(index).delete(key, rowNumber);
        }

        @Override
        boolean isOverflow() {
            return keys.size() > ORDER - 1;
        }

        @Override
        Comparable split() {
            int mid = keys.size() / 2;
            return keys.get(mid);
        }
    }

    // Leaf Node implementation
    class LeafNode extends Node {
        Map<Comparable, List<Integer>> values;
        LeafNode next;

        LeafNode() {
            super();
            this.values = new HashMap<>();
        }

        @Override
        void insert(Object key, Integer rowNumber) {
            Comparable comparableKey = (Comparable) key;
            keys.add(comparableKey);
            Collections.sort(keys);

            values.computeIfAbsent(comparableKey, k -> new ArrayList<>()).add(rowNumber);
        }

        @Override
        List<Integer> search(Object key) {
            return values.getOrDefault((Comparable) key, Collections.emptyList());
        }

        @Override
        void delete(Object key, Integer rowNumber) {
            Comparable comparableKey = (Comparable) key;
            List<Integer> rowNumbers = values.get(comparableKey);
            if (rowNumbers != null) {
                rowNumbers.remove(rowNumber);
                if (rowNumbers.isEmpty()) {
                    values.remove(comparableKey);
                    keys.remove(comparableKey);
                }
            }
        }

        @Override
        boolean isOverflow() {
            return keys.size() > ORDER - 1;
        }

        @Override
        Comparable split() {
            int mid = keys.size() / 2;
            return keys.get(mid);
        }
    }
}
