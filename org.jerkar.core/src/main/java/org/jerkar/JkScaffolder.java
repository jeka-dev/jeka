package org.jerkar;


import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIterable;

public class JkScaffolder {

	@SuppressWarnings("unchecked")
	public static JkScaffolder of(JkBuild build) {
		return new JkScaffolder(build, build.projectId().group().replace('.', '/'),
				"JkBuild",
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}

	private final JkBuild build;

	private final String packageName;

	private final String extendedClass;

	private final List<String> extraFields;

	private final List<String> extraImports;

	private final List<String> extraInit;

	private final List<String> extraDependencies;

	private final List<String> extraMethods;

	private final List<Runnable> extraActions;


	private JkScaffolder(JkBuild build, String packageName, String extendedClass,
			List<String> extraFields, List<String> extraImports,
			List<String> extraInit, List<String> extraDependencies,
			List<String> extraMethods, List<Runnable> extraActions) {
		super();
		this.build = build;
		this.packageName = packageName;
		this.extendedClass = extendedClass;
		this.extraFields = extraFields;
		this.extraImports = extraImports;
		this.extraInit = extraInit;
		this.extraDependencies = extraDependencies;
		this.extraMethods = extraMethods;
		this.extraActions = extraActions;
	}


	public void process() {
		final File spec = build.baseDir(JkBuildResolver.BUILD_SOURCE_DIR);
		spec.mkdirs();
		final Map<String, String> values = new HashMap<String, String>();
		values.put("packageName", this.packageName);
		values.put("extraImports", lines(JkUtilsIterable.withoutDoubloons(this.extraImports),""));
		values.put("extendedClass", this.extendedClass);
		values.put("extraFields", lines(this.extraFields,"    "));
		values.put("extraInit", lines(this.extraInit,"        "));
		values.put("extraDependencies", lines(this.extraDependencies,"            ."));
		values.put("extraMethods", lines(this.extraMethods,"    "));
		final File packageDir = new File(spec, packageName);
		packageDir.mkdirs();
		final File buildSource = JkUtilsFile.createFileIfNotExist(new File(packageDir,"Build.java"));
		final File template = JkUtilsFile.fromUrl(JkScaffolder.class.getResource("Build.java_sample"));
		JkUtilsFile.copyFileReplacingTokens(template, buildSource, values);
		for (final Runnable action : extraActions) {
			action.run();
		}
	}

	@SuppressWarnings("unchecked")
	public JkScaffolder withExtraAction(Runnable runnable) {
		return new JkScaffolder(build, packageName, extendedClass, extraFields, extraImports, extraInit,
				extraDependencies, extraMethods, JkUtilsIterable.concatToList(runnable, extraActions));
	}

	public JkScaffolder withExtendedClass(Class<?> clazz) {
		return this.withImports(clazz.getName()).withExtendedClass(clazz.getSimpleName());
	}

	public JkScaffolder withExtendedClass(String extendedClass) {
		return new JkScaffolder(build, packageName, extendedClass, extraFields, extraImports, extraInit,
				extraDependencies, extraMethods, extraActions);
	}

	@SuppressWarnings("unchecked")
	public JkScaffolder withInit(String ...inits) {
		final List<String> extraInits = JkUtilsIterable.concatLists(this.extraInit, Arrays.asList(inits));
		return new JkScaffolder(build, packageName, extendedClass, extraFields, extraImports, extraInits,
				extraDependencies, extraMethods, extraActions);
	}

	@SuppressWarnings("unchecked")
	public JkScaffolder withDependencies(String ...dependencies) {
		final List<String> extraDeps = JkUtilsIterable.concatLists(this.extraDependencies, Arrays.asList(dependencies));
		return new JkScaffolder(build, packageName, extendedClass, extraFields, extraImports, extraInit,
				extraDeps, extraMethods, extraActions);
	}


	public JkScaffolder withImports(String ... extraImports) {
		final List<String> imports = new LinkedList<String>(this.extraImports);
		for (final String extra : extraImports) {
			imports.add("import " + extra + ";");
		}
		return new JkScaffolder(build, packageName, extendedClass, extraFields, imports, extraInit,
				extraDependencies, extraMethods, extraActions);
	}

	public JkScaffolder withImports(Class<?> ...classes) {
		final List<String> names = new LinkedList<String>();
		for (final Class<?> clazz : classes) {
			names.add(clazz.getName());
		}
		return withImports(names.toArray(new String[0]));
	}


	private static String lines(List<String> strings, String prefix) {
		final StringBuilder builder = new StringBuilder();
		for(final String string : strings) {
			builder.append(prefix+string).append("\n");
		}
		return builder.toString();
	}





}
