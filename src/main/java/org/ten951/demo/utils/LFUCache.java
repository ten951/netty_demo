package org.ten951.demo.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 王永天
 * @version 1.0.0
 * @Description TODO
 * @createTime 2021年03月02日 15:40:00
 */
public class LFUCache<K, V> {

    /**
     * 保证存取都是O(1)
     */
    private final Map<K, CacheNode<K, V>> map;
    /**
     * 频率双向链表
     */
    private final CacheDeque<K, V>[] freqTable;

    private final int capacity;
    private final int evictionCount;
    private int curSize = 0;

    private final ReentrantLock lock = new ReentrantLock();
    private static final int DEFAULT_INITIAL_CAPACITY = 1000;

    private static final float DEFAULT_EVICTION_FACTOR = 0.75f;

    public LFUCache() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_EVICTION_FACTOR);
    }

    public LFUCache(final int maxCapacity, final float evictionFactor) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    maxCapacity);
        }
        boolean factorInRange = evictionFactor <= 1 || evictionFactor < 0;
        if (!factorInRange || Float.isNaN(evictionFactor)) {
            throw new IllegalArgumentException("Illegal eviction factor value:"
                    + evictionFactor);
        }
        this.capacity = maxCapacity;
        this.evictionCount = (int) (capacity * evictionFactor);
        this.map = new HashMap<>();
        this.freqTable = new CacheDeque[capacity + 1];
        for (int i = 0; i <= capacity; i++) {
            freqTable[i] = new CacheDeque<>();
        }
        for (int i = 0; i < capacity; i++) {
            freqTable[i].nextDeque = freqTable[i + 1];
        }
        freqTable[capacity].nextDeque = freqTable[capacity];
    }


    public int getCapacity() {
        return capacity;
    }

    public V put(final K key, final V value) {
        CacheNode<K, V> node;
        lock.lock();
        try {
            /*见过这个k*/
            if (map.containsKey(key)) {
                node = map.get(key);
                if (node != null) {
                    CacheNode.withdrawNode(node);
                }
                node.value = value;
                freqTable[0].addLastNode(node);
                map.put(key, node);
            } else {
                node = freqTable[0].addLast(key, value);
                map.put(key, node);
                curSize++;
                if (curSize > capacity) {
                    proceedEviction();
                }
            }
        } finally {
            lock.unlock();
        }
        return node.value;
    }

    public V remove(final K key) {
        CacheNode<K, V> node = null;
        lock.lock();
        try {
            if (map.containsKey(key)) {
                node = map.remove(key);
                if (node != null) {
                    CacheNode.withdrawNode(node);
                }
                curSize--;
            }
        } finally {
            lock.unlock();
        }
        return (node != null) ? node.value : null;
    }

    public V get(final K key) {
        CacheNode<K, V> node = null;
        lock.lock();
        try {
            if (map.containsKey(key)) {
                node = map.get(key);
                CacheNode.withdrawNode(node);
                node.owner.nextDeque.addLastNode(node);
            }
        } finally {
            lock.unlock();
        }
        return (node != null) ? node.value : null;
    }

    /**
     * Evicts less frequently used elements corresponding to eviction factor,
     * specified at instantiation step.
     *
     * @return number of evicted elements
     */
    private int proceedEviction() {
        int targetSize = capacity - evictionCount;
        int evictedElements = 0;

        FREQ_TABLE_ITER_LOOP:
        for (int i = 0; i <= capacity; i++) {
            CacheNode<K, V> node;
            while (!freqTable[i].isEmpty()) {
                node = freqTable[i].pollFirst();
                remove(node.key);
                if (targetSize >= curSize) {
                    break FREQ_TABLE_ITER_LOOP;
                }
                evictedElements++;
            }
        }
        return evictedElements;
    }

    /**
     * 返回当前缓存的大小
     *
     * @return cache size
     */
    public int getSize() {
        return curSize;
    }


    static class CacheNode<K, V> {

        CacheNode<K, V> prev;
        CacheNode<K, V> next;
        K key;
        V value;
        CacheDeque owner;

        CacheNode() {
        }

        CacheNode(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * 此方法接受指定的节点，并将它的邻居节点链接重新连接到彼此，因此指定的节点将不再与它们绑定。返回联合节点，如果参数为null则返回null。
         *
         * @param node note to retrieve
         * @param <K>  key
         * @param <V>  value
         * @return retrieved node
         */
        static <K, V> CacheNode<K, V> withdrawNode(
                final CacheNode<K, V> node) {
            if (node != null && node.prev != null) {
                node.prev.next = node.next;
                if (node.next != null) {
                    node.next.prev = node.prev;
                }
            }
            return node;
        }

    }

    /**
     * 双端双链表
     *
     * @param <K>
     * @param <V>
     */
    static class CacheDeque<K, V> {

        CacheNode<K, V> last;
        CacheNode<K, V> first;
        CacheDeque<K, V> nextDeque;

        /**
         * 构建链表 并初始化last和first指针
         */
        CacheDeque() {
            last = new CacheNode<>();
            first = new CacheNode<>();
            last.next = first;
            first.prev = last;
        }

        /**
         * 将指定的k,v添加到末尾. 并返回Node
         *
         * @param key   key
         * @param value value
         * @return added node
         */
        CacheNode<K, V> addLast(final K key, final V value) {
            CacheNode<K, V> node = new CacheNode<>(key, value);
            node.owner = this;
            node.next = last.next;
            node.prev = last;
            node.next.prev = node;
            last.next = node;
            return node;
        }

        CacheNode<K, V> addLastNode(final CacheNode<K, V> node) {
            node.owner = this;
            node.next = last.next;
            node.prev = last;
            node.next.prev = node;
            last.next = node;
            return node;
        }

        /**
         * 检索和移除头节点
         *
         * @return removed node
         */
        CacheNode<K, V> pollFirst() {
            CacheNode<K, V> node = null;
            if (first.prev != last) {
                node = first.prev;
                first.prev = node.prev;
                first.prev.next = first;
                node.prev = null;
                node.next = null;
            }
            return node;
        }

        /**
         * 检查是否为空.
         *
         * @return is deque empty
         */
        boolean isEmpty() {
            return last.next == first;
        }

    }


    public static void main(String[] args) {
        LFUCache<Integer, Integer> lfu = new LFUCache<>(5,0.75f);
        lfu.put(1,1);
        lfu.put(2,2);
        lfu.put(3,3);
        lfu.put(4,4);
        lfu.put(5,5);

        lfu.get(1);
        lfu.get(1);
        lfu.get(1);
        lfu.get(2);
        lfu.get(2);

        lfu.get(3);
        lfu.get(4);
        lfu.get(5);
        lfu.get(5);
        lfu.put(6,6);

        System.out.println("lfu = " + lfu);
    }
}
