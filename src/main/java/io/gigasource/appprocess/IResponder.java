package io.gigasource.appprocess;

import com.google.gson.JsonObject;

/**
 * Define methods for app_process process which created by app_process
 * to transmit data to its parent process via stdout
 */
public interface IResponder {
    /**
     * Transmit data in json string format to parent process
     * @param json
     */
    void json(String json);

    /**
     * Transmit data in json object format to parent process
     * @param json
     */
    void json(JsonObject json);


    /**
     *
     * @param content
     */
    void log(String content);
}
