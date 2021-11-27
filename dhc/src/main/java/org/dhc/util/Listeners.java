package org.dhc.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Listeners {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final Listeners instance = new Listeners();

	private static final Map<Class<? extends Event>, Set<EventListener>> listeners = new HashMap<Class<? extends Event>, Set<EventListener>>();
	
	public static Listeners getInstance() {
		return instance;
	}
	
	public void addEventListener(Class<? extends Event> eventClass, EventListener listener) {
		Set<EventListener> set = listeners.get(eventClass);
		if(set == null) {
			set = new HashSet<EventListener>();
			listeners.put(eventClass, set);
		}
		set.add(listener);

	}
	
	public void sendEvent(final Event event) {
		
		ThreadExecutor.getInstance().execute(new DhcRunnable("Process event " + event.getClass()) {
			public void doRun() {
				Set<EventListener> set = listeners.get(event.getClass());
				
				if (set == null) {
					return;
				}
				set = new HashSet<>(set);
				for (EventListener listener : set) {
					try {
						listener.onEvent(event);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		});
	}

	public void removeEventListener(Class<? extends Event> eventClass, EventListener listener) {
		Set<EventListener> set = listeners.get(eventClass);
		if(set == null) {
			return;
		}
		set.remove(listener);

	}

}
