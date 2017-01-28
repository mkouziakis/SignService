package gr.headstart.signservice.nodetypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Container for the measurements and crcs included in a resources file.
 *
 * @author KouziaMi
 */

public class RESCollection implements Iterable<String>{

    /**
     * The characters that signal the start of a comment line.
     */
    protected static final String COMMENT_CHARS = "#;";
    /**
     * The characters used to separate keys from values.
     */
    protected static final String SEPARATOR_CHARS = "=:";
    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = 2548006161386850670L;
    /**
     * Constant for the line separator.
     */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    /**
     * The characters used for quoting values.
     */
    private static final String QUOTE_CHARACTERS = "\"'";
    /**
     * The line continuation character.
     */
    private static final String LINE_CONT = "\\";
    
    public static final String MESURES_SECTION = "MESURES";
    private static final String CRC_SECTION = "CRC";
    
    private Map<String, String> measurements = new HashMap<>();
    private Map<String, String> crcs = new HashMap<>();

    public void load(Reader reader) throws Exception {
        try {
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            String currentSection = null;
            while (line != null) {
                line = line.trim();
                if (!isCommentLine(line)) {
                    if (isSectionLine(line)) {
                        String section = line.substring(1, line.length() - 1);
                        currentSection = section;
                    } else {
                        String key = "";
                        String value = "";
                        int index = findSeparator(line);
                        if (index >= 0) {
                            key = line.substring(0, index);
                            value = line.substring(index + 1);
                        } else {
                            key = line;
                        }
                        key = key.trim();
                        if (key.length() < 1) {
                            // use space for properties with no key
                            key = " ";
                        }
                        
                        switch (currentSection) {
                            case MESURES_SECTION:
                                measurements.put(key, value);
                                break;
                            case CRC_SECTION:
                                crcs.put(key, value);
                                break;
                        }
                    }
                }

                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            throw new Exception(
                    "Unable to load the configuration", e);
        }
    }

    protected boolean isCommentLine(String line) {
        if (line == null) {
            return false;
        }
        // blank lines are also treated as comment lines
        return line.length() < 1 || COMMENT_CHARS.indexOf(line.charAt(0)) >= 0;
    }

    protected boolean isSectionLine(String line) {
        if (line == null) {
            return false;
        }
        return line.startsWith("[") && line.endsWith("]");
    }

    /**
     * Tries to find the index of the separator character in the given string.
     * This method checks for the presence of separator characters in the given
     * string. If multiple characters are found, the first one is assumed to be
     * the correct separator. If there are quoting characters, they are taken
     * into account, too.
     *
     * @param line the line to be checked
     * @return the index of the separator character or -1 if none is found
     */
    private static int findSeparator(String line) {
        int index =
                findSeparatorBeforeQuote(line,
                findFirstOccurrence(line, QUOTE_CHARACTERS));
        if (index < 0) {
            index = findFirstOccurrence(line, SEPARATOR_CHARS);
        }
        return index;
    }

    /**
     * Checks for the occurrence of the specified separators in the given line.
     * The index of the first separator is returned.
     *
     * @param line the line to be investigated
     * @param separators a string with the separator characters to look for
     * @return the lowest index of a separator character or -1 if no separator
     *         is found
     */
    private static int findFirstOccurrence(String line, String separators) {
        int index = -1;

        for (int i = 0; i < separators.length(); i++) {
            char sep = separators.charAt(i);
            int pos = line.indexOf(sep);
            if (pos >= 0) {
                if (index < 0 || pos < index) {
                    index = pos;
                }
            }
        }

        return index;
    }

    /**
     * Searches for a separator character directly before a quoting character.
     * If the first non-whitespace character before a quote character is a
     * separator, it is considered the "real" separator in this line - even if
     * there are other separators before.
     *
     * @param line the line to be investigated
     * @param quoteIndex the index of the quote character
     * @return the index of the separator before the quote or &lt; 0 if there is
     *         none
     */
    private static int findSeparatorBeforeQuote(String line, int quoteIndex) {
        int index = quoteIndex - 1;
        while (index >= 0 && Character.isWhitespace(line.charAt(index))) {
            index--;
        }

        if (index >= 0 && SEPARATOR_CHARS.indexOf(line.charAt(index)) < 0) {
            index = -1;
        }

        return index;
    }
    
    public String getMeasurement(String code){
        return measurements.get(code);
    }

    public String getCRC(String code){
        return crcs.get(code);
    }
    
    @Override
    public Iterator<String> iterator() {
        return measurements.keySet().iterator();
    }
    
}
