package org.tinylog;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.tinylog.core.Framework;
import org.tinylog.core.Level;
import org.tinylog.core.Tinylog;
import org.tinylog.core.backend.LevelVisibility;
import org.tinylog.core.backend.LoggingBackend;
import org.tinylog.core.runtime.RuntimeFlavor;

/**
 * Static logger for issuing log entries.
 */
public final class Logger {

	private static final int CALLER_STACK_TRACE_DEPTH = 2;

	private static final ConcurrentMap<String, TaggedLogger> loggers = new ConcurrentHashMap<>();

	private static final Framework framework;
	private static final RuntimeFlavor runtime;
	private static final LoggingBackend backend;
	private static final LevelVisibility visibility;
	
	static {
		framework = Tinylog.getFramework();
		runtime = framework.getRuntime();
		backend = framework.getLoggingBackend();
		visibility = backend.getLevelVisibility(null);
	}

	/** */
	private Logger() {
	}

	/**
	 * Retrieves a tagged logger instance. Category tags are case-sensitive. If a tagged logger does not yet exists for
	 * the passed tag, a new logger will be created. This method always returns the same logger instance for the same
	 * tag.
	 *
	 * @param tag The case-sensitive category tag of the requested logger, or {@code null} for receiving an untagged
	 *            logger
	 * @return Logger instance
	 */
	public static TaggedLogger tag(String tag) {
		if (tag == null || tag.isEmpty()) {
			return loggers.computeIfAbsent("", key -> new TaggedLogger(null, framework));
		} else {
			return loggers.computeIfAbsent(tag, key -> new TaggedLogger(key, framework));
		}
	}

	/**
	 * Checks if the trace severity level is enabled for the actual class.
	 *
	 * <p>
	 *     If this method returns {@code true}, an issued trace log entry will be output. If this method returns
	 *     {@code false}, issued trace log entries will be discarded. 
	 * </p>
	 *
	 * @return {@code true} if enabled, otherwise {@code false}
	 */
	public static boolean isTraceEnabled() {
		return visibility.isTraceEnabled()
			&& backend.isEnabled(runtime.getStackTraceLocationAtIndex(CALLER_STACK_TRACE_DEPTH), null, Level.TRACE);
	}

	/**
	 * Checks if the debug severity level is enabled for the actual class.
	 *
	 * <p>
	 *     If this method returns {@code true}, an issued debug log entry will be output. If this method returns
	 *     {@code false}, issued debug log entries will be discarded.
	 * </p>
	 *
	 * @return {@code true} if enabled, otherwise {@code false}
	 */
	public static boolean isDebugEnabled() {
		return visibility.isDebugEnabled()
			&& backend.isEnabled(runtime.getStackTraceLocationAtIndex(CALLER_STACK_TRACE_DEPTH), null, Level.DEBUG);
	}

	/**
	 * Checks if the info severity level is enabled for the actual class.
	 *
	 * <p>
	 *     If this method returns {@code true}, an issued info log entry will be output. If this method returns
	 *     {@code false}, issued info log entries will be discarded.
	 * </p>
	 *
	 * @return {@code true} if enabled, otherwise {@code false}
	 */
	public static boolean isInfoEnabled() {
		return visibility.isInfoEnabled()
			&& backend.isEnabled(runtime.getStackTraceLocationAtIndex(CALLER_STACK_TRACE_DEPTH), null, Level.INFO);
	}

	/**
	 * Checks if the warn severity level is enabled for the actual class.
	 *
	 * <p>
	 *     If this method returns {@code true}, an issued warn log entry will be output. If this method returns
	 *     {@code false}, issued warn log entries will be discarded.
	 * </p>
	 *
	 * @return {@code true} if enabled, otherwise {@code false}
	 */
	public static boolean isWarnEnabled() {
		return visibility.isWarnEnabled()
			&& backend.isEnabled(runtime.getStackTraceLocationAtIndex(CALLER_STACK_TRACE_DEPTH), null, Level.WARN);
	}

	/**
	 * Checks if the error severity level is enabled for the actual class.
	 *
	 * <p>
	 *     If this method returns {@code true}, an issued error log entry will be output. If this method returns
	 *     {@code false}, issued error log entries will be discarded.
	 * </p>
	 *
	 * @return {@code true} if enabled, otherwise {@code false}
	 */
	public static boolean isErrorEnabled() {
		return visibility.isErrorEnabled()
			&& backend.isEnabled(runtime.getStackTraceLocationAtIndex(CALLER_STACK_TRACE_DEPTH), null, Level.ERROR);
	}

}
