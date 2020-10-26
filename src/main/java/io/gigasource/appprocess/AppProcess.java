package io.gigasource.appprocess;

import com.google.gson.JsonObject;

import java.util.Scanner;

public class AppProcess {
    private static Scanner _scanner = new Scanner(System.in);

    public static void start(IRequestHandler cb) {
        IResponder noResponseCb = new IResponder() {
            @Override
            public void json(String json) {}
            @Override
            public void json(JsonObject json) { }
        };
        while (true) {
            if (_scanner.hasNextLine()) {
                String line = _readDataFromParentProcess();
                JsonObject payload = StringHelper.toJsonObject(line);

                if (payload.has(Constants.END_SIGNAL)) {
                    return;
                }

                if (payload.has(Constants.TRANSMIT_ID)) {
                    String transmitId = payload.get(Constants.TRANSMIT_ID).getAsString();
                    cb.handle(payload, _createResponderWithId(transmitId));
                } else {
                    cb.handle(payload, noResponseCb);
                }
            }
        }
    }

    private static IResponder _createResponderWithId(String transmitId) {
        return new IResponder() {
            @Override
            public void json(String json) {
                json(StringHelper.toJsonObject(json));
            }

            @Override
            public void json(JsonObject json) {
                json.addProperty(Constants.TRANSMIT_ID, transmitId);
                _sendDataToParentProcess(json.toString());
            }
        };
    }

    private static void _sendDataToParentProcess(String data) {
        System.out.println(StringHelper._encode(data) + "\n");
    }

    private static String _readDataFromParentProcess() {
        return StringHelper._decode(_scanner.nextLine());
    }
}
