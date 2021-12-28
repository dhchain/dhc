package org.dhc.plugin;

import java.util.ServiceLoader;

import org.dhc.util.DhcLogger;

public class PluginRegistry {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final PluginRegistry instance = new PluginRegistry();
	
	public static PluginRegistry getInstance() {
		return instance;
	}
	
	public void init() {
		logger.info("init() START");

		try {
			ServiceLoader<PluginInterface> serviceLoader = ServiceLoader.load(PluginInterface.class);

			for (PluginInterface provider : serviceLoader) {
				provider.run();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		logger.info("init() END");
	}

}
