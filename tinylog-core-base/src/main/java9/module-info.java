import org.tinylog.core.Hook;
import org.tinylog.core.format.value.DateFormatBuilder;
import org.tinylog.core.format.value.JavaTimeFormatBuilder;
import org.tinylog.core.format.value.NumberFormatBuilder;
import org.tinylog.core.format.value.ValueFormatBuilder;
import org.tinylog.core.backend.LoggingBackendBuilder;
import org.tinylog.core.backend.InternalLoggingBackendBuilder;
import org.tinylog.core.backend.NopLoggingBackendBuilder;

module org.tinylog.core {
	uses Hook;

	uses ValueFormatBuilder;
	provides ValueFormatBuilder with
		DateFormatBuilder,
		JavaTimeFormatBuilder,
		NumberFormatBuilder;

	uses LoggingBackendBuilder;
	provides LoggingBackendBuilder with
		InternalLoggingBackendBuilder,
		NopLoggingBackendBuilder;

	exports org.tinylog.core;
	exports org.tinylog.core.format.message;
	exports org.tinylog.core.format.value;
	exports org.tinylog.core.backend;
	exports org.tinylog.core.runtime;
}
