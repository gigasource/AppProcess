package io.gigasource.appprocess;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

class StringHelper {
    private static final JsonParser _parser = new JsonParser();

    /**
     * Join string list to single string, separate by 'separator'
     * @param separator a string which will be insert between each string item in array
     * @param values string list
     * @return joined string
     */
    public static String join(String separator, List<String> values) {
        if (values == null || values.size() == 0)
            return "";
        if (separator == null)
            separator = "";
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for(; i<values.size()-1; ++i)
            builder.append(values.get(i)).append(separator);
        builder.append(values.get(i));
        return builder.toString();
    }

    /**
     * Join string array to single string, separate by 'separator'
     * @param separator a string which will be insert between each string item in array
     * @param values string array
     * @return joined string
     */
    public static String join(String separator, String[] values) {
        if (values == null || values.length == 0)
            return "";
        if (separator == null)
            separator = "";
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for(; i<values.length-1; ++i)
            builder.append(values[i]).append(separator);
        builder.append(values[i]);
        return builder.toString();
    }

    public static String _decode(String input) {
        return input.replace("\\\\", "\\");
    }
    public static String _encode(String input) {
        return input.replace("\\", "\\\\");
    }
    public static JsonObject toJsonObject(String input) {
        return (JsonObject) _parser.parse(input);
    }
}
