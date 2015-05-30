package org.jerkar.builtins.javabuild;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.JkLog;
import org.jerkar.file.JkFileTree;
import org.jerkar.file.JkFileTreeSet;
import org.jerkar.file.JkPathFilter;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIterable;

/**
 * This processor basically copies some resource files to a target folder (generally the class folder).
 * It can also proceed to token replacement, i.e replacing strings between <code>${</code> and <code>}</code> by a specified values.<br/>
 * The processor is constructed using a list of <code>JkDirSets</code> and for each of them, we can associate
 * a map of token to replace.<br/>
 * 
 * @author Jerome Angibaud
 */
public final class JkResourceProcessor {

	private final List<JkResourceTree> resourceTrees;

	private JkResourceProcessor(List<JkResourceTree> replacedResources) {
		super();
		this.resourceTrees = replacedResources;
	}

	/**
	 * Creates a <code>JkResourceProcessor</code> from the given <code>JkFileTreeSet</code> without processing
	 * any token replacement.
	 */
	public static JkResourceProcessor of(JkFileTreeSet treeSet) {
		final List<JkResourceTree> resourceTrees = new LinkedList<JkResourceProcessor.JkResourceTree>();
		for (final JkFileTree fileTree : treeSet.fileTrees()) {
			resourceTrees.add(JkResourceTree.of(fileTree));
		}
		return new JkResourceProcessor(resourceTrees);
	}

	/**
	 * Creates a <code>JkResourceProcessor</code> from the given <code>JkFileTree</code> without processing
	 * any token replacement.
	 */
	public static JkResourceProcessor of(JkFileTree tree) {
		return of(JkResourceTree.of(tree));
	}

	/**
	 * Creates a <code>JkResourceProcessor</code> from the given <code>JkResourceTree</code> without processing
	 * any token replacement.
	 */
	public static JkResourceProcessor of(JkResourceTree tree) {
		return new JkResourceProcessor(JkUtilsIterable.listOf(tree));
	}


	/**
	 * Actually processes the resources, meaning copies the resources to the specified output directory along
	 * replacing specified tokens.
	 */
	public void generateTo(File outputDir) {
		JkLog.startln("Coping resource files to " + outputDir.getPath());
		final Set<File> files = new HashSet<File>();
		for (final JkResourceTree resource : this.resourceTrees) {
			for (final Map.Entry<File, Map<String, String>> entry : resource.fileTokens().entrySet()) {
				final File file =entry.getKey();
				final String relativePath = resource.fileTree.relativePath(file);
				final File out = new File(outputDir, relativePath);
				JkUtilsFile.copyFileReplacingTokens(file, out, entry.getValue(), JkLog.infoStreamIfVerbose());
				files.add(JkUtilsFile.canonicalFile(file));
			}
		}
		JkLog.done(files.size() + " file(s) copied.");
	}

	/**
	 * @see JkResourceProcessor#and(JkFileTreeSet)
	 */
	public JkResourceProcessor and(JkFileTreeSet trees) {
		JkResourceProcessor result = this;
		for (final JkFileTree fileTree : trees.fileTrees()) {
			result = result.and(fileTree);
		}
		return result;
	}

	/**
	 * @see JkResourceProcessor#and(JkFileTreeSet)
	 */
	public JkResourceProcessor and(JkFileTree tree) {
		return and(JkResourceTree.of(tree));
	}

	/**
	 * @see JkResourceProcessor#and(JkFileTreeSet)
	 */
	public JkResourceProcessor andIfExist(File ...dirs) {
		return and(JkFileTreeSet.of(dirs));
	}


	/**
	 * Creates a <code>JkResourceProcessor</code> from this one and adding the specified <code>JkFileTreeSet</code>
	 * along its token to be replaced.
	 */
	public JkResourceProcessor and(JkResourceTree resourceSet) {
		final List<JkResourceTree> list = new LinkedList<JkResourceTree>(this.resourceTrees);
		list.add(resourceSet);
		return new JkResourceProcessor(list);
	}

	/**
	 * @see JkResourceProcessor#and(JkFileTreeSet)
	 */
	public JkResourceProcessor and(JkFileTree tree, Map<String, String> tokenReplacement) {
		return and(JkResourceTree.of(tree).and(JkInterpolator.of(JkPathFilter.ACCEPT_ALL, tokenReplacement)));
	}

	/**
	 * Creates a <code>JkResourceProcessor</code> identical at this one but adding the specified interpolator.
	 */
	public JkResourceProcessor and(JkInterpolator interpolator) {
		final List<JkResourceTree> list = new LinkedList<JkResourceProcessor.JkResourceTree>();
		for (final JkResourceTree resourceTree : this.resourceTrees) {
			list.add(resourceTree.and(interpolator));
		}
		return new JkResourceProcessor(list);
	}

	/**
	 * Shorthand for {@link #and(JkInterpolator)}.
	 * @see {@link JkInterpolator#of(String, String, String, String...)}
	 */
	public JkResourceProcessor interpolating(String includeFilter, String key, String value, String...others) {
		return and(JkInterpolator.of(includeFilter, key, value, others));
	}



	/**
	 * A {@link JkFileTree} along information to replace token on given files.
	 * You can associate a file tree to a list of replacement. A replacement consists
	 * in a {@link JkPathFilter} and a {@link Map} of <code>String</code>.<p>
	 * For each file matching the filter,
	 * 
	 * 
	 * @author Jerome Angibaud
	 */
	public static final class JkResourceTree {

		private final JkFileTree fileTree;
		private final List<JkInterpolator> jkInterpolators;

		private JkResourceTree(JkFileTree fileTree, List<JkInterpolator> jkInterpolators) {
			super();
			this.fileTree = fileTree;
			this.jkInterpolators = jkInterpolators;
		}

		public static JkResourceTree of(JkFileTree fileTree) {
			return new JkResourceTree(fileTree, new LinkedList<JkResourceProcessor.JkInterpolator>());
		}

		@Override
		public String toString() {
			return fileTree + "  " + jkInterpolators;
		}

		public JkResourceTree and(JkInterpolator interpolator) {
			final List<JkInterpolator> jkInterpolators = new LinkedList<JkInterpolator>(this.jkInterpolators);
			jkInterpolators.add(interpolator);
			return new JkResourceTree(fileTree, jkInterpolators);
		}

		private Map<File, Map<String, String>> fileTokens() {
			if (!this.fileTree.root().exists()) {
				return new HashMap<File, Map<String,String>>();
			}
			final Map<File, Map<String, String>> map = new HashMap<File, Map<String,String>>();
			for (final File file : fileTree) {
				map.put(file, new HashMap<String, String>());
			}
			for (final JkInterpolator jkInterpolator : this.jkInterpolators) {
				final JkFileTree tree = this.fileTree.andFilter(jkInterpolator.fileFilter);
				for (final File file : tree) {
					final Map<String, String> keyValues = map.get(file);
					keyValues.putAll(jkInterpolator.keyValues);
				}
			}
			return map;
		}

	}

	/**
	 * Defines values to be interpolated (replacing <code>${key}</code> by their value),
	 * and the file filter to apply it.
	 */
	public static class JkInterpolator {

		private final Map<String, String> keyValues;

		private final JkPathFilter fileFilter;

		private JkInterpolator(JkPathFilter fileFilter, Map<String, String> keyValues) {
			super();
			this.keyValues = keyValues;
			this.fileFilter = fileFilter;
		}

		/**
		 * Creates a <code>JkInterpolator</code> with the specified filter and key/values to replace.
		 */
		public static JkInterpolator of(JkPathFilter filter, Map<String, String> map) {
			return new JkInterpolator(filter, new HashMap<String, String>(map));
		}

		/**
		 * Same as {@link #of(JkPathFilter, Map)} but you can specify key values in line.
		 */
		public static JkInterpolator of(JkPathFilter filter, String key, String value, String... others) {
			return new JkInterpolator(filter, JkUtilsIterable.mapOf(key, value, (Object[]) others));
		}

		/**
		 * Same as {@link #of(JkPathFilter, String, String, String...))} but specify an include pattern that will be used as the path filter.
		 */
		public static JkInterpolator of(String includeFilter, String key, String value, String... others) {
			return of(JkPathFilter.include(includeFilter), key, value, others);
		}

		/**
		 * Same as {@link #of(JkPathFilter, Map)} but you can specify key values in line.
		 */
		public static JkInterpolator of(String includeFilter, Map<String, String> map) {
			return new JkInterpolator(JkPathFilter.include(includeFilter), map);
		}

		/**
		 * Returns a copy of this {@link JkInterpolator} but adding key values to interpolate
		 */
		public JkInterpolator and(String key, String value, String... others) {
			final Map<String, String> map = JkUtilsIterable.mapOf(key, value, (Object[]) others);
			map.putAll(keyValues);
			return new JkInterpolator(this.fileFilter, map);
		}

	}

}
