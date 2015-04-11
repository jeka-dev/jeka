package org.jerkar.java;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.JkDir;
import org.jerkar.JkDirSet;
import org.jerkar.JkLog;
import org.jerkar.utils.JkUtilsIterable;

/**
 * This processor basically copies some resource files to a target folder (generally the class folder).
 * It can also proceed to token replacement, i.e replacing strings between <code>${</code> and <code>}</code> by a specified values.<br/>
 * The processor is constructed using a list of <code>JakeDirSets</code> and for each of them, we can associate
 * a map of token to replace.<br/>
 * 
 * @author Jerome Angibaud
 */
public final class JkResourceProcessor {

	private final List<ResourceDirSet> resourceDirSet;

	private JkResourceProcessor(List<ResourceDirSet> replacedResources) {
		super();
		this.resourceDirSet = replacedResources;
	}

	/**
	 * Creates a <code>JkResourceProcessor</code> from the given <code>JkDirSet</code> without processing
	 * any token replacement.
	 */
	@SuppressWarnings("unchecked")
	public static JkResourceProcessor of(JkDirSet dirSet) {
		return new JkResourceProcessor(JkUtilsIterable.listOf(new ResourceDirSet(dirSet, Collections.EMPTY_MAP)));
	}

	/**
	 * Creates a <code>JkResourceProcessor</code> from the given <code>JkDirSet</code> and processes
	 * replacement on specified token. <br/>
	 * For example, if you want to replace,  <code>${date}</code> by <code>2015-01-30</code> then you have to fill the specified
	 * <code>tokenValues</code> map with the following entry : <code>"date" -> "2015-01-30"</code> (without the quotes).
	 */
	public static JkResourceProcessor of(JkDirSet dirSet, Map<String, String> tokenValues) {
		return new JkResourceProcessor(JkUtilsIterable.listOf(new ResourceDirSet(dirSet, new HashMap<String, String>(tokenValues))));
	}

	/**
	 * Creates a <code>JkResourceProcessor</code> from the given <code>JkDirSet</code> and processes
	 * replacement on specified tokens by the specified values. <br/>
	 * Its just a shorthand for {@link #of(JkDirSet, Map)} specifying tokens/values map in-line.
	 */
	public static JkResourceProcessor of(JkDirSet dirSet, String token, String value, String ...others) {
		return of(dirSet, JkUtilsIterable.mapOf(token, value, (Object[]) others));
	}

	/**
	 * Actually processes the resources, meaning copies the resources to the specified output directory along
	 * replacing specified tokens.
	 */
	public void generateTo(File outputDir) {
		JkLog.startln("Coping resource files to " + outputDir.getPath());
		int count = 0;
		for (final ResourceDirSet resource : this.resourceDirSet) {
			count = count + resource.dirSet.copyRepacingTokens(outputDir, resource.replacement);
		}
		JkLog.done(count + " file(s) copied.");
	}

	public JkResourceProcessor with(Map<String, String> replacement) {
		final List<ResourceDirSet> list = new LinkedList<JkResourceProcessor.ResourceDirSet>();
		for (final ResourceDirSet resourceDirSet : this.resourceDirSet) {
			list.add(resourceDirSet.and(replacement));
		}
		return new JkResourceProcessor(list);
	}

	public JkResourceProcessor with(String replacedToken, String value) {
		return this.with(JkUtilsIterable.mapOf(replacedToken, value));
	}

	/**
	 * @see JkResourceProcessor#and(JkDirSet)
	 */
	@SuppressWarnings("unchecked")
	public JkResourceProcessor and(JkDirSet dirSet) {
		return and(dirSet, Collections.EMPTY_MAP);
	}

	/**
	 * @see JkResourceProcessor#and(JkDirSet)
	 */
	public JkResourceProcessor and(JkDir dir) {
		return and(JkDirSet.of(dir));
	}

	/**
	 * @see JkResourceProcessor#and(JkDirSet)
	 */
	public JkResourceProcessor andIfExist(File ...dirs) {
		return and(JkDirSet.of(dirs));
	}

	/**
	 * @see JkResourceProcessor#and(JkDirSet)
	 */
	public JkResourceProcessor andIfExist(Map<String, String> replacedValues, File ...dirs) {
		return and(JkDirSet.of(dirs), replacedValues);
	}

	/**
	 * Creates a <code>JkResourceProcessor</code> from this one and adding the specified <code>JkDirSet</code>
	 * along its token to be replaced.
	 */
	public JkResourceProcessor and(JkDirSet dirSet, Map<String, String> tokenReplacement) {
		final List<ResourceDirSet> list = new LinkedList<ResourceDirSet>(this.resourceDirSet);
		list.add(new ResourceDirSet(dirSet, new HashMap<String, String>(tokenReplacement)));
		return new JkResourceProcessor(list);
	}

	/**
	 * @see JkResourceProcessor#and(JkDirSet)
	 */
	public JkResourceProcessor and(JkDir dir, Map<String, String> tokenReplacement) {
		return and(JkDirSet.of(dir), tokenReplacement);
	}


	private static class ResourceDirSet {

		public final JkDirSet dirSet;
		public final Map<String, String> replacement;

		public ResourceDirSet(JkDirSet dirSet, Map<String, String> replacement) {
			super();
			this.dirSet = dirSet;
			this.replacement = replacement;
		}

		@Override
		public String toString() {
			return dirSet.toString() + "  " + replacement.toString();
		}

		public ResourceDirSet and(Map<String, String> replacement) {
			final Map<String, String> map = new HashMap<String, String>(this.replacement);
			map.putAll(replacement);
			return new ResourceDirSet(dirSet, map);
		}

	}

}
