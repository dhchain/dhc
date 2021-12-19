package org.dhc.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ExpiringMap<K, V> implements Map<K, V> {

	private static final DhcLogger logger = DhcLogger.getLogger();

	private Map<K, V> map =  new ConcurrentHashMap<K, V>();
	private Map<K, ScheduledFuture<?>> futures =  new ConcurrentHashMap<K, ScheduledFuture<?>>();
	private long defaultTimeout;
	
	public ExpiringMap() {
		this(Constants.MINUTE);
	} 
	
	public ExpiringMap(long defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	} 

	public int size() {
		return map.size();
	}

	public synchronized boolean isEmpty() {
		return map.isEmpty();
	}

	public synchronized boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public synchronized V get(Object key) {
		return map.get(key);
	}

	public synchronized V put(K key, V value) {
		return put(key, value, defaultTimeout);
	}
	
	public V put(K key, V value, long timeout) {
		ScheduledFuture<?> scheduledFuture = futures.get(key);
		if(scheduledFuture != null) {
			scheduledFuture.cancel(false);
		}
		scheduledFuture = ThreadExecutor.getInstance().schedule(new DhcRunnable("ExpiringMap") {
			
			@Override
			public void doRun() {
				futures.remove(key);
				V value = map.remove(key);
				if(value instanceof Expiring) {
					((Expiring)value).expire();
				}
				logger.trace("Removed key={} value={}", key, value);
				
			}
		}, timeout);
		futures.put(key, scheduledFuture);
		return map.put(key, value);
	}

	public synchronized V remove(Object key) {
		ScheduledFuture<?> scheduledFuture = futures.remove(key);
		if(scheduledFuture != null) {
			scheduledFuture.cancel(true);
		}
		return map.remove(key);
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}

	public synchronized void clear() {
		for(ScheduledFuture<?> scheduledFuture: futures.values()) {
			if(scheduledFuture != null) {
				scheduledFuture.cancel(true);
			}
		}
		futures.clear();
		map.clear();
	}

	public synchronized Set<K> keySet() {
		return map.keySet();
	}

	public Collection<V> values() {
		return map.values();
	}

	public Set<Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	public boolean equals(Object o) {
		return map.equals(o);
	}

	public int hashCode() {
		return map.hashCode();
	}

	public V getOrDefault(Object key, V defaultValue) {
		return map.getOrDefault(key, defaultValue);
	}

	public void forEach(BiConsumer<? super K, ? super V> action) {
		map.forEach(action);
	}

	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		map.replaceAll(function);
	}

	public V putIfAbsent(K key, V value) {
		return map.putIfAbsent(key, value);
	}

	public boolean remove(Object key, Object value) {
		return map.remove(key, value);
	}

	public boolean replace(K key, V oldValue, V newValue) {
		return map.replace(key, oldValue, newValue);
	}

	public V replace(K key, V value) {
		return map.replace(key, value);
	}

	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		return map.computeIfAbsent(key, mappingFunction);
	}

	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return map.computeIfPresent(key, remappingFunction);
	}

	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return map.compute(key, remappingFunction);
	}

	public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return map.merge(key, value, remappingFunction);
	}



	


}
