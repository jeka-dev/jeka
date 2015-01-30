package org.jake.java;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.JakeDir;
import org.jake.JakeDirSet;
import org.jake.JakeLog;
import org.jake.utils.JakeUtilsIterable;

/**
 * This processor basically copies some resource files to a target folder (generally the class folder).
 * It can also proceed to token replacement, i.e replacing strings between <code>${</code> and <code>}</code> by a specified values.<br/>
 * The processor is constructed using a list of <code>JakeDirSets</code> and for each of them, we can associate
 * a map of token to replace.<br/>
 * 
 * @author Jerome Angibaud
 */
public final class JakeResourceProcessor {

    private final List<ResourceDirSet> resourceDirSet;

    private JakeResourceProcessor(List<ResourceDirSet> replacedResources) {
        super();
        this.resourceDirSet = replacedResources;
    }

    /**
     * Creates a <code>JakeResourceProcessor</code> from the given <code>JakeDirSet</code> without processing
     * any token replacement.
     */
    @SuppressWarnings("unchecked")
    public static JakeResourceProcessor of(JakeDirSet dirSet) {
        return new JakeResourceProcessor(JakeUtilsIterable.listOf(new ResourceDirSet(dirSet, Collections.EMPTY_MAP)));
    }

    /**
     * Creates a <code>JakeResourceProcessor</code> from the given <code>JakeDirSet</code> and processes
     * replacement on specified token. <br/>
     * For example, if you want to replace,  <code>${date}</code> by <code>2015-01-30</code> then you have to fill the specified
     * <code>tokenValues</code> map with the following entry : <code>"date" -> "2015-01-30"</code> (without the quotes).
     */
    public static JakeResourceProcessor of(JakeDirSet dirSet, Map<String, String> tokenValues) {
        return new JakeResourceProcessor(JakeUtilsIterable.listOf(new ResourceDirSet(dirSet, new HashMap<String, String>(tokenValues))));
    }

    /**
     * Creates a <code>JakeResourceProcessor</code> from the given <code>JakeDirSet</code> and processes
     * replacement on specified tokens by the specified values. <br/>
     * Its just a shorthand for {@link #of(JakeDirSet, Map)} specifying tokens/values map in-line.
     */
    public static JakeResourceProcessor of(JakeDirSet dirSet, String token, String value, String ...others) {
        return of(dirSet, JakeUtilsIterable.mapOf(token, value, others));
    }

    /**
     * Actually processes the resources, meaning copies the resources to the specified output directory along
     * replacing specified tokens.
     */
    public void generateTo(File outputDir) {
        JakeLog.startln("Coping resource files to " + outputDir.getPath());
        int count = 0;
        for (final ResourceDirSet resource : this.resourceDirSet) {
            count = count + resource.dirSet.copyRepacingTokens(outputDir, resource.replacement);
        }
        JakeLog.done(count + " file(s) copied.");
    }

    /**
     * @see JakeResourceProcessor#and(JakeDirSet)
     */
    @SuppressWarnings("unchecked")
    public JakeResourceProcessor and(JakeDirSet dirSet) {
        return and(dirSet, Collections.EMPTY_MAP);
    }

    /**
     * @see JakeResourceProcessor#and(JakeDirSet)
     */
    public JakeResourceProcessor and(JakeDir dir) {
        return and(JakeDirSet.of(dir));
    }

    /**
     * @see JakeResourceProcessor#and(JakeDirSet)
     */
    public JakeResourceProcessor andIfExist(File ...dirs) {
        return and(JakeDirSet.of(dirs));
    }

    /**
     * @see JakeResourceProcessor#and(JakeDirSet)
     */
    public JakeResourceProcessor andIfExist(Map<String, String> replacedValues, File ...dirs) {
        return and(JakeDirSet.of(dirs), replacedValues);
    }

    /**
     * Creates a <code>JakeResourceProcessor</code> from this one and adding the specified <code>JakeDirSet</code>
     * along its token to be replaced.
     */
    public JakeResourceProcessor and(JakeDirSet dirSet, Map<String, String> tokenReplacement) {
        final List<ResourceDirSet> list = new LinkedList<ResourceDirSet>(this.resourceDirSet);
        list.add(new ResourceDirSet(dirSet, new HashMap<String, String>(tokenReplacement)));
        return new JakeResourceProcessor(list);
    }

    /**
     * @see JakeResourceProcessor#and(JakeDirSet)
     */
    public JakeResourceProcessor and(JakeDir dir, Map<String, String> tokenReplacement) {
        return and(JakeDirSet.of(dir), tokenReplacement);
    }


    private static class ResourceDirSet {

        public final JakeDirSet dirSet;
        public final Map<String, String> replacement;

        public ResourceDirSet(JakeDirSet dirSet, Map<String, String> replacement) {
            super();
            this.dirSet = dirSet;
            this.replacement = replacement;
        }

    }

}
