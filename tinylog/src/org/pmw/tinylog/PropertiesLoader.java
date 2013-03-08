/*
 * Copyright 2012 Martin Winandy
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.pmw.tinylog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

import org.pmw.tinylog.labellers.Labeller;
import org.pmw.tinylog.policies.Policy;
import org.pmw.tinylog.writers.LoggingWriter;

/**
 * Loads configuration for {@link Logger} from properties.
 */
final class PropertiesLoader {

	private static final String LEVEL_PROPERTY = "tinylog.level";
	private static final String FORMAT_PROPERTY = "tinylog.format";
	private static final String LOCALE_PROPERTY = "tinylog.locale";
	private static final String STACKTRACE_PROPERTY = "tinylog.stacktrace";
	private static final String WRITER_PROPERTY = "tinylog.writer";
	private static final String WRITING_THREAD_PROPERTY = "tinylog.writingthread";
	private static final String WRITING_THREAD_OBSERVE_PROPERTY = WRITING_THREAD_PROPERTY + ".observe";
	private static final String WRITING_THREAD_PRIORITY_PROPERTY = WRITING_THREAD_PROPERTY + ".priority";

	private static final String SERVICES_PREFIX = "META-INF/services/";
	private static final String PACKAGE_LEVEL_PREFIX = LEVEL_PROPERTY + ":";

	private PropertiesLoader() {
	}

	/**
	 * Load configuration from properties.
	 * 
	 * @param properties
	 *            Properties with configuration
	 * @return A new configurator
	 */
	static Configurator readProperties(final Properties properties) {
		Configurator configurator = Configurator.defaultConfig();
		readProperties(configurator, properties);
		return configurator;
	}

	/**
	 * Load configuration from properties.
	 * 
	 * @param configurator
	 *            Confirgurator to update
	 * @param properties
	 *            Properties with configuration
	 */
	static void readProperties(final Configurator configurator, final Properties properties) {
		String level = properties.getProperty(LEVEL_PROPERTY);
		if (level != null && !level.isEmpty()) {
			try {
				configurator.level(LoggingLevel.valueOf(level.toUpperCase(Locale.ENGLISH)));
			} catch (IllegalArgumentException ex) {
				// Ignore
			}
		}

		Enumeration<Object> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.startsWith(PACKAGE_LEVEL_PREFIX)) {
				String packageName = key.substring(PACKAGE_LEVEL_PREFIX.length());
				String value = properties.getProperty(key);
				try {
					LoggingLevel loggingLevel = LoggingLevel.valueOf(value.toUpperCase(Locale.ENGLISH));
					configurator.level(packageName, loggingLevel);
				} catch (IllegalArgumentException ex) {
					// Illegal logging level => reset
					configurator.level(packageName, null);
				}
			}
		}

		String format = properties.getProperty(FORMAT_PROPERTY);
		if (format != null && !format.isEmpty()) {
			configurator.formatPattern(format);
		}

		String localeString = properties.getProperty(LOCALE_PROPERTY);
		if (localeString != null && !localeString.isEmpty()) {
			String[] localeArray = localeString.split("_", 3);
			if (localeArray.length == 1) {
				configurator.locale(new Locale(localeArray[0]));
			} else if (localeArray.length == 2) {
				configurator.locale(new Locale(localeArray[0], localeArray[1]));
			} else if (localeArray.length >= 3) {
				configurator.locale(new Locale(localeArray[0], localeArray[1], localeArray[2]));
			}
		}

		String stacktace = properties.getProperty(STACKTRACE_PROPERTY);
		if (stacktace != null && !stacktace.isEmpty()) {
			try {
				int limit = Integer.parseInt(stacktace);
				configurator.maxStackTraceElements(limit);
			} catch (NumberFormatException ex) {
				// Ignore
			}
		}

		String writer = properties.getProperty(WRITER_PROPERTY);
		if (writer != null && !writer.isEmpty()) {
			if (writer.equalsIgnoreCase("null")) {
				configurator.writer(null);
			} else {
				for (Class<?> implementation : findImplementations(LoggingWriter.class)) {
					if (LoggingWriter.class.isAssignableFrom(implementation)) {
						if (writer.equalsIgnoreCase(getName(implementation))) {
							LoggingWriter loggingWriter = loadAndSetWriter(properties, implementation);
							if (loggingWriter != null) {
								configurator.writer(loggingWriter);
								break;
							}
						}
					}
				}
			}
		}

		String writingThread = properties.getProperty(WRITING_THREAD_PROPERTY);
		if ("true".equalsIgnoreCase(writingThread) || "1".equalsIgnoreCase(writingThread)) {
			String observedThread = properties.getProperty(WRITING_THREAD_OBSERVE_PROPERTY);
			boolean observedThreadDefined = observedThread != null;
			if (observedThreadDefined && observedThread.equalsIgnoreCase("null")) {
				observedThread = null;
			}
			String priorityString = properties.getProperty(WRITING_THREAD_PRIORITY_PROPERTY);
			Integer priority;
			if (priorityString == null) {
				priority = null;
			} else {
				try {
					priority = Integer.parseInt(priorityString.trim());
				} catch (NumberFormatException ex) {
					priority = null;
				}
			}
			if (priority != null && observedThreadDefined) {
				configurator.writingThread(observedThread, priority);
			} else if (priority != null) {
				configurator.writingThread(priority);
			} else if (observedThreadDefined) {
				configurator.writingThread(observedThread);
			} else {
				configurator.writingThread(true);
			}
		} else {
			configurator.writingThread(false);
		}
	}

	private static Collection<Class<?>> findImplementations(final Class<?> service) {
		try {
			Enumeration<URL> urls = PropertiesLoader.class.getClassLoader().getResources(SERVICES_PREFIX + service.getPackage().getName());
			if (urls == null || !urls.hasMoreElements()) {
				return Collections.emptyList();
			} else {
				Collection<Class<?>> services = new ArrayList<Class<?>>();
				while (urls.hasMoreElements()) {
					URL url = urls.nextElement();
					InputStream inputStream = null;
					BufferedReader reader = null;
					try {
						inputStream = url.openStream();
						reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
						for (String line = reader.readLine(); line != null; line = reader.readLine()) {
							line = line.trim();
							if (line != null) {
								try {
									services.add(Class.forName(line));
								} catch (ClassNotFoundException ec) {
									// Continue
								}
							}
						}
					} finally {
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (IOException ex) {
								// Ignore
							}
						}
						if (reader != null) {
							try {
								reader.close();
							} catch (IOException ex) {
								// Ignore
							}
						}
					}
				}
				return services;
			}
		} catch (IOException ex) {
			return Collections.emptyList();
		}
	}

	private static LoggingWriter loadAndSetWriter(final Properties properties, final Class<?> writerClass) {
		try {
			String[][] supportedProperties = getSupportedProperties(writerClass);
			Constructor<?> foundConstructor = null;
			Object[] foundParameters = null;
			for (Constructor<?> constructor : writerClass.getConstructors()) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				String[] propertiesNames = findPropertyNames(supportedProperties, parameterTypes.length);
				if (propertiesNames != null) {
					if (foundParameters == null || foundParameters.length < parameterTypes.length) {
						Object[] parameters = loadParameters(properties, propertiesNames, parameterTypes);
						if (parameters != null) {
							foundConstructor = constructor;
							foundParameters = parameters;
						}
					}
				}
			}
			if (foundConstructor != null) {
				return (LoggingWriter) foundConstructor.newInstance(foundParameters);
			} else {
				return null;
			}
		} catch (InstantiationException ex) {
			return null;
		} catch (IllegalAccessException ex) {
			return null;
		} catch (IllegalArgumentException ex) {
			return null;
		} catch (InvocationTargetException ex) {
			return null;
		}
	}

	private static String getName(final Class<?> clazz) {
		try {
			Method method = clazz.getMethod("getName");
			if (method.getReturnType() == String.class && Modifier.isStatic(method.getModifiers())) {
				return (String) method.invoke(null);
			} else {
				return null;
			}
		} catch (Exception ex) {
			return null;
		}
	}

	private static String[][] getSupportedProperties(final Class<?> clazz) {
		try {
			Method method = clazz.getMethod("getSupportedProperties");
			if (method.getReturnType() == String[][].class && Modifier.isStatic(method.getModifiers())) {
				return (String[][]) method.invoke(null);
			} else {
				return new String[][] { new String[] {} };
			}
		} catch (Exception ex) {
			return new String[][] { new String[] {} };
		}
	}

	private static String[] findPropertyNames(final String[][] supportedProperties, final int numParameters) {
		for (String[] propertyNames : supportedProperties) {
			if (propertyNames.length == numParameters) {
				return propertyNames;
			}
		}
		return null;
	}

	private static Object[] loadParameters(final Properties properties, final String[] propertyNames, final Class<?>[] parameterTypes) {
		Object[] parameters = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; ++i) {
			String name = propertyNames[i];
			String value = properties.getProperty(WRITER_PROPERTY + "." + name);
			if (value != null) {
				Class<?> type = parameterTypes[i];
				if (String.class.equals(type)) {
					parameters[i] = value;
				} else if (int.class.equals(type)) {
					try {
						parameters[i] = Integer.parseInt(value);
					} catch (NumberFormatException ex) {
						return null;
					}
				} else if (boolean.class.equals(type)) {
					parameters[i] = "true".equalsIgnoreCase(value) || "1".equalsIgnoreCase(value);
				} else if (Labeller.class.equals(type)) {
					parameters[i] = parse(Labeller.class, value);
				} else if (Policy.class.equals(type)) {
					parameters[i] = parse(Policy.class, value);
				} else if (Policy[].class.equals(type)) {
					parameters[i] = parsePolicies(value);
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		return parameters;
	}

	private static Policy[] parsePolicies(final String string) {
		List<Policy> policies = new ArrayList<Policy>();
		for (String part : string.split(Pattern.quote(","))) {
			Policy policy = (Policy) parse(Policy.class, part.trim());
			if (policy != null) {
				policies.add(policy);
			}
		}
		return policies.toArray(new Policy[0]);
	}

	private static Object parse(final Class<?> service, final String string) {
		int separator = string.indexOf(':');
		String name = separator > 0 ? string.substring(0, separator).trim() : string.trim();
		String parameter = separator > 0 ? string.substring(separator + 1).trim() : null;

		for (Class<?> implementation : findImplementations(service)) {
			if (service.isAssignableFrom(implementation)) {
				if (name.equalsIgnoreCase(getName(implementation))) {
					return createInstance(implementation, parameter);
				}
			}
		}

		return null;
	}

	private static Object createInstance(final Class<?> clazz, final String parameter) {
		try {
			if (parameter != null) {
				try {
					Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
					constructor.setAccessible(true);
					return constructor.newInstance(parameter);
				} catch (NoSuchMethodException ex) {
					// Continue
				}
			}
			Constructor<?> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (InstantiationException ex) {
			return null;
		} catch (IllegalAccessException ex) {
			return null;
		} catch (IllegalArgumentException ex) {
			return null;
		} catch (InvocationTargetException ex) {
			return null;
		} catch (SecurityException ex) {
			return null;
		} catch (NoSuchMethodException ex) {
			return null;
		}
	}

}
