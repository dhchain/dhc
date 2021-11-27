package org.dhc.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DoubleMap<K1, K2, V> {
	
	private Map<K1, Map<K2, V>> map = new LinkedHashMap<>();
	
	public V get(K1 key1, K2 key2) {
		Map<K2, V> submap = map.get(key1);
		if(submap == null) {
			return null;
		}
		return submap.get(key2);
	}
	
	public V put(K1 key1, K2 key2, V value) {
		Map<K2, V> submap = map.get(key1);
		if(submap == null) {
			submap = new HashMap<>();
			map.put(key1, submap);
		}
		return submap.put(key2, value);
	}
	
	public void clear() {
		map.clear();
	}
	
	public Map<K1, V> getBySecondKey(K2 key2) {
		Map<K1, V> submap = new HashMap<K1, V>();
		for(K1 key1: map.keySet()) {
			Map<K2,V> m = map.get(key1);
			V value = m.get(key2);
			if(value != null) {
				submap.put(key1, value);
			}
		}
		return submap;
	}
	
	public Map<K2, V> getByFirstKey(K1 key1) {
		return map.get(key1);
	}
	
	public Map<K2, V> removeByFirstKey(K1 key1) {
		return map.remove(key1);
	}
	
	public void remove(K1 key1, K2 key2) {
		Map<K2, V> submap = map.get(key1);
		if(submap == null) {
			return;
		}
		
		submap.remove(key2);
		
		if(submap.isEmpty()) {
			map.remove(key1);
		}
	}

	public Set<K1> getFirstKeys() {
		return map.keySet();
	}
	
	public int size() {
		return map.size();
	}

}
