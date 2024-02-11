package dev.jeka.core.api.text;

import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class that allows formatting text into two columns.
 * Each column consists of a left and right section separated by a delimiter.
 */
public class Jk2ColumnsText {

    private final int leftSize;

    private final int maxSize;

    private final String delimiter;

    private boolean adjustLeft;

    private String marginLeft = "";

    public final List<Row> rows = new LinkedList<>();

    private Jk2ColumnsText(int leftSize, int maxSize, String delimiter) {
        this.leftSize = leftSize;
        this.maxSize = maxSize;
        this.delimiter = delimiter;
    }

    /**
     * Creates a new instance of Jk2ColumnsText with the given parameters.
     *
     * @param leftSize the size of the left column
     * @param maxSize the maximum overall size of each row in the text
     * @param delimiter the delimiter used to separate the columns
     */
    public static Jk2ColumnsText of(int leftSize, int maxSize, String delimiter) {
        return new Jk2ColumnsText(leftSize, maxSize, delimiter);
    }

    /**
     * Creates a new instance of Jk2ColumnsText with the given left size and maximum size.
     * Uses the delimiter " : ".
     *
     * @see #of(int, int, String)
     */
    public static Jk2ColumnsText of(int leftSize, int maxSize) {
        return new Jk2ColumnsText(leftSize, maxSize, " : ");
    }

    /**
     * Adds a new row with the given left and right values.
     */
    public Jk2ColumnsText add(String left, Object right) {
        rows.add(new Row(left, Objects.toString(right)));
        return this;
    }

    /**
     * Sets the adjustLeft flag. When set to true, the left column will adjust its size to accommodate
     * longer left values. When set to false, the left column will be truncated to the specified size.
     * <p>
     * Initial value is <code>true</code>
     */
    public Jk2ColumnsText setAdjustLeft(boolean adjustLeft) {
        this.adjustLeft = adjustLeft;
        return this;
    }

    /**
     * Sets the left margin for displaying the whole result;
     *
     * @param marginLeft the left margin to set
     * @return the modified Jk2ColumnsText object
     */
    public Jk2ColumnsText setMarginLeft(String marginLeft) {
        this.marginLeft = marginLeft;
        return this;
    }

    @Override
    public String toString() {
        int effectiveLeftSize = adjustLeft ? Integer.min(largestOkLeft(), leftSize)
                : leftSize;
        String result = rows.stream()
                .map(row -> row.format(effectiveLeftSize, maxSize, delimiter))
                .flatMap(List::stream)
                .map(line -> marginLeft + line)
                .reduce("", (init, line) -> init + line + "\n");
        return result.substring(0, result.length()-1); // remove last '\n'
    }

    private int largestOkLeft() {
        return rows.stream()
                .map(row -> row.left.length())
                .filter(length -> length <= leftSize)
                .max(Integer::compare).orElse(1000);
    }

    private static class Row {

        final String left;

        final String right;

        Row(String left, String right) {
            this.left = left;
            this.right = right;
        }

        List<String> format(int leftLength, int maxLength, String separator) {
            List<String> result = new LinkedList<>();
            int rightStartIndex = leftLength + separator.length();
            String rightPad = JkUtilsString.repeat(" ", rightStartIndex);
            int rightLength = maxLength - rightStartIndex;

            // If left exceed left size
            if (left.length() > leftLength) {
                result.add(left + separator);
                wrap(right, rightLength).map(line -> rightPad + line).forEach(result::add);

            } else {
                List<String> rightLines = wrap(right, rightLength).collect(Collectors.toCollection(LinkedList::new));
                result.add(JkUtilsString.padEnd(left, leftLength, ' ') + separator + rightLines.get(0));
                rightLines.remove(0);
                rightLines.forEach(line -> result.add(rightPad + line));
            }
            return result;
        }

        private Stream<String> wrap(String original, int size) {
            return Arrays.stream(JkUtilsString.wrapStringCharacterWise(original, size).split("\n"));
        }
    }


}
