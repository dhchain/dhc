package org.dhc.util;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Configurator {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final String APP_PROPERTIES = "config/dhc.properties";
	private static final Configurator instance = new Configurator();
	

	private Properties props = null;

	private Configurator() {
		props = new Properties();
		try {
			Path filePath = Paths.get(APP_PROPERTIES);
			if (!Files.exists(filePath)) {
				try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
					writer.newLine();
					writer.close();
				}
			}
			props.load(new FileInputStream(APP_PROPERTIES));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static Configurator getInstance() {
		return instance;
	}

	public String getProperty(String key) {
		return (String) props.get(key);
	}
	
	public int getIntProperty(String key) {
		String str = getProperty(key);
		if(str == null) {
			return 0;
		}
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			return 0;
		}
	}
	
	public int getIntProperty(String key, int defaultValue) {
		int result = getIntProperty(key);
		result = result == 0 ? defaultValue : result;
		return result;
	}
	
	public boolean getBooleanProperty(String key) {
		String str = getProperty(key);
		if("true".equals(str)) {
			return true;
		}
		return false;
	}

}