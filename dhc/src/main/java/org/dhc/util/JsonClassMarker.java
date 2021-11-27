package org.dhc.util;

public class JsonClassMarker {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private String type;

	public JsonClassMarker(Class<? extends Message> type) {
		this.type = type.getName();
	}

	public Class<? extends Message> getType() {
		if(type == null) {
			return null;
		}
		try {
			return Class.forName(type).asSubclass(Message.class);
		} catch (ClassNotFoundException e) {
			logger.trace("java.lang.ClassNotFoundException: {}", e.getMessage());
			return null;
		}
	}

}
