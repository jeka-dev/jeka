package org.jake;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jake.utils.JakeUtilsIterable;

public class JakeOptions {

	private static final JakeOptions instance = new JakeOptions();

	private final PropertyCollector propCollector;

	private final boolean verbose;


	protected JakeOptions() {
		this(PropertyCollector.systemProps());
	}

	protected JakeOptions(PropertyCollector props) {
		this.propCollector = props;
		this.verbose = props.boolOr("jake.verbose", false,
				"Set it to true to display more details in console.");
	}


	public static boolean isVerbose() {
		return instance.verbose;
	}

	@Override
	public String toString() {
		return propCollector.toStringValues();
	}

	public static final class PropertyCollector {

		private final Properties properties;

		private final List<PropDefinition> definitions = new LinkedList<JakeOptions.PropDefinition>();

		public static PropertyCollector systemProps() {
			final Properties properties = new Properties(System.getProperties());
			for (final Object name : System.getProperties().keySet()) {
				final String key = (String) name;
				if (key.startsWith("java.") || key.startsWith("sun.") || key.startsWith("user.")
						|| key.equals("path.separator") || key.equals("file.encoding")
						|| key.equals("os.arch") || key.equals("file.encoding.pkg")) {
					properties.remove(name);
					System.out.println("s++++++++ sys remove " + name);
				} else {
					System.out.println("s++++++++ sys keeeeeeeeeeeeeeeeeeeeeep " + name);
				}
			}
			return new PropertyCollector(properties);
		}

		public PropertyCollector(Properties properties) {
			this.properties = properties;
		}

		public boolean boolOr(String name, boolean defaultValue,
				String definition) {
			definitions.add(new PropDefinition(name, defaultValue, definition,
					Boolean.class));
			final boolean present = properties.containsKey(name);
			if (!present) {
				return defaultValue;
			}
			final String value = properties.getProperty(name);
			if (value == null || value.equals("")) {
				return true;
			}
			return Boolean.parseBoolean(value);
		}

		public String stringOr(String name, String defaultValue,
				String definition) {
			definitions.add(new PropDefinition(name, defaultValue, definition,
					String.class));
			final boolean present = properties.containsKey(name);
			if (!present) {
				return defaultValue;
			}
			final String value = properties.getProperty(name);
			if (value == null) {
				return defaultValue;
			}
			return value;
		}


		public String toStringValues() {
			return this.properties.toString();
		}

		@Override
		public String toString() {
			final List<String> strings = new LinkedList<String>();
			for (final PropDefinition propertyDef : this.definitions) {
				final String name = propertyDef.name;
				final String value = properties.getProperty(name);
				if (value!= null) {
					strings.add(propertyDef.name + " = " + value);
				}
			}
			return JakeUtilsIterable.toString(strings, ";");
		}

		public String toStringDetails() {
			final Set<Object> remainings = new HashSet<Object>(this.properties.keySet());
			final Iterator<Object> it = remainings.iterator();
			while (it.hasNext()) {
				final String name = (String) it.next();
				if (!name.startsWith("jake.")) {
					it.remove();
				}
			}
			final List<String> strings = new LinkedList<String>();
			for (final PropDefinition propertyDef : this.definitions) {
				final String name = propertyDef.name;
				final String value = properties.getProperty(name);
				strings.add(propertyDef.name + "=" + propertyDef.valueOrDefault(value));
			}
			final StringBuilder builder = new StringBuilder(JakeUtilsIterable.toString(strings, ";"));
			builder.append(" (Unused props :" + JakeUtilsIterable.toString(remainings, ";") + ")");
			return builder.toString();
		}



	}

	protected static final class PropDefinition implements
	Comparable<PropDefinition> {

		private final String name;

		private final Object defaultValue;

		private final String definition;

		private final String type;

		@SuppressWarnings("rawtypes")
		public PropDefinition(String name, Object defaultValue,
				String definition, Class type) {
			super();
			this.name = name;
			this.defaultValue = defaultValue;
			this.definition = definition;
			this.type = type.getSimpleName();
		}

		public Object valueOrDefault(Object value) {
			if (value == null) {
				return defaultValue;
			}
			return value;
		}

		@Override
		public String toString() {
			return name + " : " + definition + " Type = " + type
					+ ", defauft value = " + defaultValue;
		}

		@Override
		public int compareTo(PropDefinition other) {
			this.name.compareTo(other.name);
			return 0;
		}

	}

}
