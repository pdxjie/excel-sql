package com.excelsql.engine.index;

import java.io.Serializable;
import java.util.*;

import java.io.Serializable;
import java.util.*;

/**
 * B+树索引实现
 * 特点：
 * 1. 所有数据都存储在叶子节点中
 * 2. 叶子节点通过链表连接，支持范围查询
 * 3. 内部节点只存储键值，用于导航
 * 4. 支持重复键值
 *
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:42
 */
public class BPlusTreeIndex implements Serializable {

    /** B+树的阶数，决定了每个节点最多能存储的键值数量 */
    private static final int ORDER = 4;

    /** 最大键值数量 = ORDER - 1 */
    private static final int MAX_KEYS = ORDER - 1;

    /** 最小键值数量 = ORDER / 2 - 1 (除了根节点) */
    private static final int MIN_KEYS = ORDER / 2 - 1;

    /** 根节点 */
    private Node root;

    /**
     * 构造函数，初始化B+树
     */
    public BPlusTreeIndex() {
        this.root = new LeafNode();
    }

    /**
     * 插入键值对
     * @param key 键值
     * @param rowNumber 行号
     */
    public void insert(Object key, Integer rowNumber) {
        if (key == null || rowNumber == null) {
            throw new IllegalArgumentException("键值和行号不能为空");
        }

        Comparable comparableKey = (Comparable) key;
        root.insert(comparableKey, rowNumber);

        // 如果根节点溢出，需要创建新的根节点
        if (root.isOverflow()) {
            InternalNode newRoot = new InternalNode();
            Node[] splitResult = root.split();

            newRoot.keys.add(splitResult[1].getFirstKey());
            newRoot.children.add(splitResult[0]);
            newRoot.children.add(splitResult[1]);

            root = newRoot;
        }
    }

    /**
     * 搜索指定键值对应的所有行号
     * @param key 要搜索的键值
     * @return 包含该键值的所有行号列表
     */
    public List<Integer> search(Object key) {
        if (key == null) {
            return Collections.emptyList();
        }

        Comparable comparableKey = (Comparable) key;
        return root.search(comparableKey);
    }

    /**
     * 删除指定键值和行号的记录
     * @param key 键值
     * @param rowNumber 行号
     */
    public void delete(Object key, Integer rowNumber) {
        if (key == null || rowNumber == null) {
            return;
        }

        Comparable comparableKey = (Comparable) key;
        root.delete(comparableKey, rowNumber);

        // 如果根节点是内部节点且只有一个子节点，则将子节点提升为新的根节点
        if (root instanceof InternalNode && root.keys.isEmpty() && !((InternalNode) root).children.isEmpty()) {
            root = ((InternalNode) root).children.get(0);
        }
    }

    /**
     * 范围查询：查找指定范围内的所有行号
     * @param startKey 起始键值（包含）
     * @param endKey 结束键值（包含）
     * @return 范围内所有行号的列表
     */
    public List<Integer> rangeSearch(Object startKey, Object endKey) {
        if (startKey == null || endKey == null) {
            return Collections.emptyList();
        }

        Comparable start = (Comparable) startKey;
        Comparable end = (Comparable) endKey;

        if (start.compareTo(end) > 0) {
            return Collections.emptyList();
        }

        return root.rangeSearch(start, end);
    }

    /**
     * 获取B+树的高度
     * @return 树的高度
     */
    public int getHeight() {
        return root.getHeight();
    }

    /**
     * 打印B+树结构（用于调试）
     */
    public void printTree() {
        System.out.println("B+树结构：");
        root.print(0);
    }

    // ==================== 节点抽象类 ====================

    /**
     * B+树节点抽象类
     */
    abstract class Node implements Serializable {
        /** 存储键值的列表，始终保持有序 */
        List<Comparable> keys;

        /**
         * 构造函数
         */
        Node() {
            this.keys = new ArrayList<>();
        }

        /**
         * 插入键值对
         * @param key 键值
         * @param rowNumber 行号
         */
        abstract void insert(Comparable key, Integer rowNumber);

        /**
         * 搜索指定键值
         * @param key 要搜索的键值
         * @return 包含该键值的所有行号列表
         */
        abstract List<Integer> search(Comparable key);

        /**
         * 删除指定键值和行号的记录
         * @param key 键值
         * @param rowNumber 行号
         */
        abstract void delete(Comparable key, Integer rowNumber);

        /**
         * 判断节点是否溢出
         * @return true表示溢出，需要分裂
         */
        abstract boolean isOverflow();

        /**
         * 判断节点是否下溢
         * @return true表示下溢，需要合并或重新分配
         */
        abstract boolean isUnderflow();

        /**
         * 分裂节点
         * @return 分裂后的两个节点数组 [左节点, 右节点]
         */
        abstract Node[] split();

        /**
         * 获取节点的第一个键值
         * @return 第一个键值
         */
        abstract Comparable getFirstKey();

        /**
         * 范围查询
         * @param startKey 起始键值
         * @param endKey 结束键值
         * @return 范围内所有行号的列表
         */
        abstract List<Integer> rangeSearch(Comparable startKey, Comparable endKey);

        /**
         * 获取节点高度
         * @return 节点高度
         */
        abstract int getHeight();

        /**
         * 打印节点结构
         * @param level 当前层级
         */
        abstract void print(int level);

        /**
         * 使用二分查找定位键值应该插入的位置
         * @param key 要查找的键值
         * @return 插入位置的索引
         */
        protected int findInsertPosition(Comparable key) {
            // 解决泛型类型问题
            @SuppressWarnings("unchecked")
            int index = Collections.binarySearch((List<Comparable<Object>>) (List<?>) keys, key);
            return index < 0 ? -index - 1 : index;
        }

        /**
         * 使用二分查找定位键值的位置（用于搜索）
         * @param key 要查找的键值
         * @return 键值位置的索引
         */
        protected int findSearchPosition(Comparable key) {
            // 解决泛型类型问题
            @SuppressWarnings("unchecked")
            int index = Collections.binarySearch((List<Comparable<Object>>) (List<?>) keys, key);
            return index < 0 ? -index - 1 : index;
        }
    }

    // ==================== 内部节点类 ====================

    /**
     * B+树内部节点实现
     * 内部节点不存储实际数据，只存储键值用于导航到子节点
     */
    class InternalNode extends Node {
        /** 子节点列表，children.size() = keys.size() + 1 */
        List<Node> children;

        /**
         * 构造函数
         */
        InternalNode() {
            super();
            this.children = new ArrayList<>();
        }

        @Override
        void insert(Comparable key, Integer rowNumber) {
            int index = findSearchPosition(key);
            Node child = children.get(index);
            child.insert(key, rowNumber);

            // 如果子节点溢出，需要分裂
            if (child.isOverflow()) {
                Node[] splitResult = child.split();

                // 更新当前子节点
                children.set(index, splitResult[0]);

                // 插入新的子节点和对应的键值
                Comparable newKey = splitResult[1].getFirstKey();
                keys.add(index, newKey);
                children.add(index + 1, splitResult[1]);
            }
        }

        @Override
        List<Integer> search(Comparable key) {
            int index = findSearchPosition(key);
            return children.get(index).search(key);
        }

        @Override
        void delete(Comparable key, Integer rowNumber) {
            int index = findSearchPosition(key);
            children.get(index).delete(key, rowNumber);

            // 删除后可能需要调整树结构，这里简化处理
            // 在实际应用中，可能需要处理节点下溢的情况
        }

        @Override
        boolean isOverflow() {
            return keys.size() > MAX_KEYS;
        }

        @Override
        boolean isUnderflow() {
            return keys.size() < MIN_KEYS;
        }

        @Override
        Node[] split() {
            int mid = keys.size() / 2;

            // 创建新的右节点
            InternalNode rightNode = new InternalNode();

            // 分割键值：右节点获取mid+1到末尾的键值
            rightNode.keys.addAll(keys.subList(mid + 1, keys.size()));
            // 分割子节点：右节点获取mid+1到末尾的子节点
            rightNode.children.addAll(children.subList(mid + 1, children.size()));

            // 当前节点保留0到mid-1的键值和0到mid的子节点
            keys.subList(mid, keys.size()).clear();
            children.subList(mid + 1, children.size()).clear();

            return new Node[]{this, rightNode};
        }

        @Override
        Comparable getFirstKey() {
            return children.get(0).getFirstKey();
        }

        @Override
        List<Integer> rangeSearch(Comparable startKey, Comparable endKey) {
            List<Integer> result = new ArrayList<>();

            // 找到起始位置
            int startIndex = findSearchPosition(startKey);

            // 从起始位置开始搜索
            for (int i = startIndex; i < children.size(); i++) {
                List<Integer> childResult = children.get(i).rangeSearch(startKey, endKey);
                result.addAll(childResult);

                // 如果当前子树的最小键值已经大于endKey，可以提前结束
                if (i < keys.size() && keys.get(i).compareTo(endKey) > 0) {
                    break;
                }
            }

            return result;
        }

        @Override
        int getHeight() {
            return 1 + (children.isEmpty() ? 0 : children.get(0).getHeight());
        }

        @Override
        void print(int level) {
            String indent = "  ".repeat(level);
            System.out.println(indent + "InternalNode: " + keys);
            for (Node child : children) {
                child.print(level + 1);
            }
        }
    }

    // ==================== 叶子节点类 ====================

    /**
     * B+树叶子节点实现
     * 叶子节点存储实际的数据（行号列表），并通过next指针连接形成链表
     */
    class LeafNode extends Node {
        /** 存储键值到行号列表的映射 */
        Map<Comparable, List<Integer>> values;

        /** 指向下一个叶子节点的指针，用于范围查询 */
        LeafNode next;

        /**
         * 构造函数
         */
        LeafNode() {
            super();
            this.values = new LinkedHashMap<>(); // 使用LinkedHashMap保持插入顺序
            this.next = null;
        }

        @Override
        void insert(Comparable key, Integer rowNumber) {
            // 将键值添加到keys列表中并保持有序
            if (!keys.contains(key)) {
                int position = findInsertPosition(key);
                keys.add(position, key);
            }

            // 将行号添加到对应键值的列表中
            values.computeIfAbsent(key, k -> new ArrayList<>()).add(rowNumber);
        }

        @Override
        List<Integer> search(Comparable key) {
            List<Integer> result = values.get(key);
            return result != null ? new ArrayList<>(result) : Collections.emptyList();
        }

        @Override
        void delete(Comparable key, Integer rowNumber) {
            List<Integer> rowNumbers = values.get(key);
            if (rowNumbers != null) {
                rowNumbers.remove(rowNumber);

                // 如果该键值没有对应的行号了，则完全删除该键值
                if (rowNumbers.isEmpty()) {
                    values.remove(key);
                    keys.remove(key);
                }
            }
        }

        @Override
        boolean isOverflow() {
            return keys.size() > MAX_KEYS;
        }

        @Override
        boolean isUnderflow() {
            return keys.size() < MIN_KEYS;
        }

        @Override
        Node[] split() {
            int mid = keys.size() / 2;

            // 创建新的右叶子节点
            LeafNode rightNode = new LeafNode();

            // 分割键值：右节点获取mid到末尾的键值
            rightNode.keys.addAll(keys.subList(mid, keys.size()));

            // 分割数据：将对应的键值对移动到右节点
            for (int i = mid; i < keys.size(); i++) {
                Comparable key = keys.get(i);
                rightNode.values.put(key, values.get(key));
            }

            // 从当前节点中删除已移动的数据
            for (int i = keys.size() - 1; i >= mid; i--) {
                Comparable key = keys.get(i);
                values.remove(key);
            }
            keys.subList(mid, keys.size()).clear();

            // 更新链表指针
            rightNode.next = this.next;
            this.next = rightNode;

            return new Node[]{this, rightNode};
        }

        @Override
        Comparable getFirstKey() {
            return keys.isEmpty() ? null : keys.get(0);
        }

        @Override
        List<Integer> rangeSearch(Comparable startKey, Comparable endKey) {
            List<Integer> result = new ArrayList<>();

            // 遍历当前叶子节点的所有键值
            for (Comparable key : keys) {
                if (key.compareTo(startKey) >= 0 && key.compareTo(endKey) <= 0) {
                    result.addAll(values.get(key));
                } else if (key.compareTo(endKey) > 0) {
                    break; // 键值已经超出范围，可以结束
                }
            }

            // 继续搜索后续的叶子节点
            LeafNode current = this.next;
            while (current != null) {
                boolean hasValidKey = false;
                for (Comparable key : current.keys) {
                    if (key.compareTo(endKey) <= 0) {
                        if (key.compareTo(startKey) >= 0) {
                            result.addAll(current.values.get(key));
                        }
                        hasValidKey = true;
                    } else {
                        break;
                    }
                }

                // 如果当前节点没有有效键值，说明已经超出范围
                if (!hasValidKey) {
                    break;
                }

                current = current.next;
            }

            return result;
        }

        @Override
        int getHeight() {
            return 1; // 叶子节点高度为1
        }

        @Override
        void print(int level) {
            String indent = "  ".repeat(level);
            System.out.println(indent + "LeafNode: " + keys + " -> " + values);
        }
    }
}