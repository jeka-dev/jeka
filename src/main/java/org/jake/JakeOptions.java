package org.jake;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class JakeOptions {

	private static final JakeOptions instance;


	static {
		instance = new JakeOptions();
	}

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
			return new PropertyCollector(System.getProperties());
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
			if (value == null) {
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
