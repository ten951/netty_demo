package org.ten951.demo.cache;

import org.ten951.demo.utils.LFUCache;

/**
 * @author 王永天
 * @version 1.0.0
 * @Description TODO
 * @createTime 2021年03月03日 15:56:00
 */
public class LfuCache implements Cache {

    private final LFUCache store;

    public LfuCache(Integer maxSize, Float evictionFactor) {
        this.store = new LFUCache<>(maxSize, evictionFactor);
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
