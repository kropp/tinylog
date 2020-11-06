package org.tinylog.core.backend;

import java.util.Collections;
import java.util.List;

import org.tinylog.core.Level;
import org.tinylog.core.format.message.MessageFormatter;
import org.tinylog.core.runtime.StackTraceLocation;

/**
 * Wrapper for multiple {@link LoggingBackend LoggingBackends}.
 */
public class BundleLoggingBackend implements LoggingBackend {

	private final List<LoggingBackend> backends;

	/**
	 * @param backends Logging backends to combine
	 */
	public BundleLoggingBackend(List<LoggingBackend> backends) {
		this.backends = backends;
	}

	/**
	 * Gets all wrapped child logging backends.
	 *
	 * @return The wrapped child logging backends
	 */
	public List<LoggingBackend> getProviders() {
		return Collections.unmodifiableList(backends);
	}

	@Override
	public void log(StackTraceLocation location, String tag, Level level, Throwable throwable, Object message,
			Object[] arguments, MessageFormatter formatter) {
		StackTraceLocation childLocation = location.push();
		for (LoggingBackend backend : backends) {
			backend.log(childLocation, tag, level, throwable, message, arguments, formatter);
		}
	}

}
