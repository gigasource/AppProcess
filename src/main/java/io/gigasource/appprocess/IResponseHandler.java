package io.gigasource.appprocess;

import com.google.gson.JsonObject;

/**
 * Define method which will be use in parent process to handle response from app_process process
 */
public interface IResponseHandler {
    void handle(JsonObject data);
}
