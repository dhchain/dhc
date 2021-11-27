package org.dhc.util;

import org.tinylog.Level;
import org.tinylog.configuration.Configuration;
import org.tinylog.format.AdvancedMessageFormatter;
import org.tinylog.format.MessageFormatter;
import org.tinylog.provider.LoggingProvider;
import org.tinylog.provider.ProviderRegistry;

public class DhcLogger {
	
	private static final int STACKTRACE_DEPTH = 2;
	
	private static final MessageFormatter formatter = new AdvancedMessageFormatter(
			Configuration.getLocale(),
			Configuration.isEscapingEnabled()
		);

	private static final LoggingProvider provider = ProviderRegistry.getLoggingProvider();
	
	// @formatter:off
		private static final boolean MINIMUM_LEVEL_COVERS_TRACE = isCoveredByMinimumLevel(Level.TRACE);
		private static final boolean MINIMUM_LEVEL_COVERS_DEBUG = isCoveredByMinimumLevel(Level.DEBUG);
		private static final boolean MINIMUM_LEVEL_COVERS_INFO  = isCoveredByMinimumLevel(Level.INFO);
		private static final boolean MINIMUM_LEVEL_COVERS_WARN  = isCoveredByMinimumLevel(Level.WARN);
		private static final boolean MINIMUM_LEVEL_COVERS_ERROR = isCoveredByMinimumLevel(Level.ERROR);
		// @formatter:on
	
	public static DhcLogger getLogger() {
		return new DhcLogger();
	}

	public void debug(String message) {
		if (MINIMUM_LEVEL_COVERS_DEBUG) {
			provider.log(STACKTRACE_DEPTH, null, Level.DEBUG, null, formatter, message, (Object[]) null);
		}
	}

	public void debug(String message, Object... arguments) {
		if (MINIMUM_LEVEL_COVERS_DEBUG) {
			provider.log(STACKTRACE_DEPTH, null, Level.DEBUG, null, formatter, message, arguments);
		}
	}
	
	public void debug(String message, Throwable e) {
		if (MINIMUM_LEVEL_COVERS_DEBUG) {
			provider.log(STACKTRACE_DEPTH, null, Level.DEBUG, e, formatter, message, (Object[]) null);
		}
	}
	
	private static boolean isCoveredByMinimumLevel(final Level level) {
		return provider.getMinimumLevel(null).ordinal() <= level.ordinal();
	}

	public void error(String message, Throwable e) {
		if (MINIMUM_LEVEL_COVERS_ERROR) {
			provider.log(STACKTRACE_DEPTH, null, Level.ERROR, e, formatter, message, (Object[]) null);
		}
	}

	public void error(String message, Object... arguments) {
		if (MINIMUM_LEVEL_COVERS_ERROR) {
			provider.log(STACKTRACE_DEPTH, null, Level.ERROR, null, formatter, message, arguments);
		}
		
	}
	
	public void info(String message, Throwable e) {
		if (MINIMUM_LEVEL_COVERS_INFO) {
			provider.log(STACKTRACE_DEPTH, null, Level.INFO, e, formatter, message, (Object[]) null);
		}
	}

	public void info(String message, Object... arguments) {
		if (MINIMUM_LEVEL_COVERS_INFO) {
			provider.log(STACKTRACE_DEPTH, null, Level.INFO, null, formatter, message, arguments);
		}
	}
	
	public void trace(String message, Object... arguments) {
		if (MINIMUM_LEVEL_COVERS_TRACE) {
			provider.log(STACKTRACE_DEPTH, null, Level.TRACE, null, formatter, message, arguments);
		}
	}
	
	public void trace(String message, Throwable e) {
		if (MINIMUM_LEVEL_COVERS_TRACE) {
			provider.log(STACKTRACE_DEPTH, null, Level.TRACE, e, formatter, message, (Object[]) null);
		}
	}
	
	public void warn(String message, Object... arguments) {
		if (MINIMUM_LEVEL_COVERS_WARN) {
			provider.log(STACKTRACE_DEPTH, null, Level.WARN, null, formatter, message, arguments);
		}
	}
	
	public void warn(String message, Throwable e) {
		if (MINIMUM_LEVEL_COVERS_WARN) {
			provider.log(STACKTRACE_DEPTH, null, Level.WARN, e, formatter, message, (Object[]) null);
		}
	}

}
