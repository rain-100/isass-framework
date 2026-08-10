// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import vip.isass.framework.common.support.StringPool;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一键多值的双向映射集合
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class MultiValueBiMap<K, V> implements Multimap<K, V> {

    private final Multimap<K, V> multiValueMap = HashMultimap.create();

    private final Map<V, K> reverseMap = new ConcurrentHashMap<>();

    @Override
    public int size() {
        return multiValueMap.size();
    }

    @Override
    public boolean isEmpty() {
        return multiValueMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return multiValueMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        //noinspection SuspiciousMethodCalls
        return reverseMap.containsKey(value);
    }

    @Override
    public boolean containsEntry(Object key, Object value) {
        return multiValueMap.containsEntry(key, value);
    }

    @Override
    public Collection<V> get(K key) {
        return multiValueMap.get(key);
    }

    public K getKey(Object value) {
        //noinspection unchecked
        return reverseMap.get((V) value);
    }

    @Override
    public boolean put(K key, V value) {
        synchronized (StringPool.intern(key.toString())) {
            multiValueMap.put(key, value);
        }
        reverseMap.put(value, key);
        return true;
    }

    @Override
    public boolean putAll(K key, Iterable<? extends V> values) {
        synchronized (StringPool.intern(key.toString())) {
            multiValueMap.putAll(key, values);
        }
        for (V value : values) {
            reverseMap.put(value, key);
        }
        return true;
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
        for (Map.Entry<? extends K, ? extends V> entry : multimap.entries()) {
            synchronized (StringPool.intern(entry.getKey().toString())) {
                multiValueMap.put(entry.getKey(), entry.getValue());
            }
            reverseMap.put(entry.getValue(), entry.getKey());
        }
        return true;
    }

    @Override
    public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
        Collection<V> existingValues;
        synchronized (StringPool.intern(key.toString())) {
            existingValues = multiValueMap.replaceValues(key, values);
        }
        if (existingValues != null) {
            for (V existingValue : existingValues) {
                reverseMap.remove(existingValue, key);
            }
        }

        for (V value : values) {
            reverseMap.put(value, key);
        }
        return existingValues;
    }

    @Override
    public Collection<V> removeAll(Object key) {
        Collection<V> existingValues;
        synchronized (StringPool.intern(key.toString())) {
            existingValues = multiValueMap.removeAll(key);
        }
        if (existingValues == null) {
            return Collections.emptyList();
        }
        for (V existingValue : existingValues) {
            reverseMap.remove(existingValue, key);
        }
        return existingValues;
    }

    @Override
    public boolean remove(Object key, Object value) {
        boolean removed;
        synchronized (StringPool.intern(key.toString())) {
            removed = multiValueMap.remove(key, value);
        }
        if (removed) {
            //noinspection SuspiciousMethodCalls
            reverseMap.remove(value, key);
        }
        return removed;
    }

    public boolean removeValue(Object value) {
        //noinspection SuspiciousMethodCalls
        K removed = reverseMap.remove(value);
        if (removed != null) {
            synchronized (StringPool.intern(removed.toString())) {
                multiValueMap.remove(removed, value);
            }
        }
        return removed == null;
    }

    public boolean removeValues(Object key, Iterable<? extends V> values) {
        boolean removed = false;
        for (V value : values) {
            removed |= remove(key, value);
        }
        return removed;
    }

    @Override
    public void clear() {
        synchronized (this) {
            multiValueMap.clear();
        }
        reverseMap.clear();
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return multiValueMap.entries();
    }

    @Override
    public Multiset<K> keys() {
        return multiValueMap.keys();
    }

    @Override
    public Set<K> keySet() {
        return multiValueMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return multiValueMap.values();
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return multiValueMap.asMap();
    }

    @Override
    public String toString() {
        return multiValueMap.toString();
    }
}
