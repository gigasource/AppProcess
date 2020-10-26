package io.gigasource.appprocess;

import com.google.gson.JsonObject;

/**
 * Define an interface which its implements will be used in app_process process
 * to handle request from parent process
 */
public interface IRequestHandler {
    void handle(JsonObject data, IResponder response);
}
