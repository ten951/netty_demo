package org.ten951.demo.cache;

/**
 * @author 王永天
 * @version 1.0.0
 * @Description TODO
 * @createTime 2021年03月02日 17:12:00
 */
public interface Cache {

    void put(Object key, Object value);

    Object get(Object key);
}
