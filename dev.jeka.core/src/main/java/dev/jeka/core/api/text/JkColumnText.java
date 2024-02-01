package dev.jeka.core.api.text;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.*;

/**
 * Utility class for formatting text into columns.
 */
public final class JkColumnText {

    private String separator = "  ";

    private int numColumns;

    private final List<Integer> minColumnSizes = new LinkedList<>();

    private final List<Integer> maxColumnSizes = new LinkedList<>();

    private final List<String[]> rows = new LinkedList<>();

    private JkColumnText() {
    }

    /**
     * Creates a new instance with a single column, specified by the minimum and maximum sizes.
     */
    public static JkColumnText ofSingle(int minSize, int maxSize) {
        JkColumnText result = new JkColumnText();
        result.addColumn(minSize, maxSize);
        return result;
    }

    /**
     * Adds a column to this object with the specified minimum and maximum sizes.
     */
    public JkColumnText addColumn(int minSize, int maxSize) {
        JkUtilsAssert.argument(minSize <= maxSize, "Max size %s can't be lesser than min size %s", maxSize, minSize);
        numColumns ++;
        minColumnSizes.add(minSize);
        maxColumnSizes.add(maxSize);
        return this;
    }

    /**
     * Adds a column to this object with the specified size.
     */
    public JkColumnText addColumn(int size) {
        return addColumn(size, size);
    }

    /**
     * Sets the separator to separate columns.
     */
    public JkColumnText setSeparator(String separator) {
        this.separator = separator;
        return this;
    }

    /**
     * Adds a row by providing text for each column.
     */
    public JkColumnText add(String ... row) {
        rows.add(row);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Looking for each row
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {

            // Inside each row we may need to have several nested lines
            int nestedLineCount = lineCountForRow(rowIndex);
            for (int nestedLineIndex = 0; nestedLineIndex < nestedLineCount; nestedLineIndex++) {

                // Compute the content of each nested line
                String[] row = rows.get(rowIndex);
                for (int columnIndex = 0; columnIndex< numColumns; columnIndex++) {
                    int columnSize = computeColumnSize(columnIndex);
                    String cellText = row[columnIndex];
                    String[] wrappedCellTextLines = JkUtilsString.wrapStringCharacterWise(cellText, columnSize)
                            .split("\n");

                    String wrappedLine = nestedLineIndex >= wrappedCellTextLines.length ? ""
                            : wrappedCellTextLines[nestedLineIndex];
                    String padded = JkUtilsString.padEnd(wrappedLine,  columnSize, ' ');
                    sb.append(padded).append(separator);
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private int lineCountForRow(int rowIndex) {
        String[] row = this.rows.get(rowIndex);
        int result = 1;
        for (int i =0; i < numColumns; i++) {
            int columnSize = computeColumnSize(i);
            String originalText = row[i];
            String wrappedText = JkUtilsString.wrapStringCharacterWise(originalText, columnSize);
            int lineCount = wrappedText.split("\n").length;
            result = Math.max(lineCount, result);
        }
        return result;
    }

    private int computeColumnSize(int columnIndex) {
        int minSize = minColumnSizes.get(columnIndex);
        int maxSize = maxColumnSizes.get(columnIndex);
        int longestText = maxTextSize(columnIndex);
        int sup = Integer.min(maxSize, longestText);
        return Integer.min(sup, minSize);
    }

    private int maxTextSize(int columnIndex) {
        return rows.stream()
                .map(row -> row[columnIndex])
                .map(String::length)
                .max(Comparator.naturalOrder()).orElse(0);
    }


}