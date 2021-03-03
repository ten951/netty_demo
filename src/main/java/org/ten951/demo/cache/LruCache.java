package org.ten951.demo.cache;

import org.ten951.demo.utils.LRUCache;

import java.util.Map;

/**
 * @author 王永天
 * @version 1.0.0
 * @Description TODO
 * @createTime 2021年03月03日 15:48:00
 */
public class LruCache implements Cache {

    private final Map<Object, Object> store;

    public LruCache(Integer maxSize) {
        this.store = new LRUCache<>(maxSize);
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return store.get(key);
    }
}
