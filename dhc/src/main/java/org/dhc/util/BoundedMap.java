package org.dhc.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BoundedMap<K, V> {
	
	private Queue<K> queue = new ConcurrentLinkedQueue<K>();
	private Map<K, V> map =  new HashMap<K, V>(); 
	private int maxSize;
	
	public BoundedMap(int maxSize) {
		this.maxSize = maxSize;
	}
	
	public synchronized V get(K key) {
		clean();
		return map.get(key);
	}
	public synchronized V put(K key, V value) {
		clean();
		queue.add(key);
		return map.put(key, value);
	}
	
	private void clean() {
		while (true) {
			if(map.size() < maxSize) {
				return;
			}
			K key = queue.poll();
			map.remove(key);
		}
	}

	public synchronized boolean containsKey(K key) {
		clean();
		return map.containsKey(key);
	}

	public synchronized Set<K> keySet() {
		clean();
		return map.keySet();
	}

	public synchronized Collection<V> values() {
		clean();
		return map.values();
	}
	
	public synchronized void clear() {
		queue.clear();
		map.clear();
	}

	public synchronized void remove(K key) {
		queue.remove(key);
		map.remove(key);
	}

}
