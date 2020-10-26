package io.gigasource.appprocess;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class AppProcessHost {
    private Process process;
    private DataOutputStream stdin;
    // Response
    private DataInputStream stdout;
    private Thread stdOutReaderThread;
    private HashMap<String, IResponseHandler> callbacks;
    private static final JsonParser _parser = new JsonParser();

    /**
     * Create AppProcessHost process with specified entry point
     * @param publicSourceDir applicationContext.getApplicationInfo().publicSourceDir
     * @param entryPoint AppProcess entryPoint class (a class which have public static void main(String[] args) and call only AppProcess.start(..) inside it
     */
    public static AppProcessHost create(Class<?> entryPoint, String publicSourceDir) throws Exception {
        // TODO: simplify app_process creation
        // the method should only allow entry point class as an only argument
        // public source dir should be generated somehow
        File scanSrcDir = new File(new File(publicSourceDir).getParent());
        String scanSrcDirPath = scanSrcDir.getPath() + "/";
        File[] listOfFiles = scanSrcDir.listFiles();
        List<String> apkFilePaths = new ArrayList<>();
        for (File file : listOfFiles) {
            if (file.getName().endsWith("apk"))
                apkFilePaths.add(scanSrcDirPath + file.getName());
        }
        return new AppProcessHost(StringHelper.join(":", apkFilePaths), entryPoint.getName());
    }
    private AppProcessHost(String classPath, String entryCls) throws IOException {
        process = Runtime.getRuntime().exec("sh");
        stdout = new DataInputStream((process.getInputStream()));
        callbacks = new HashMap<>();
        stdOutReaderThread = new Thread(() -> {
            while(true) {
                try {
                    JsonObject response = _readDataFromAppProcess();
                    if (response != null) {
                        String cmdId = response.get(Constants.TRANSMIT_ID).getAsString();
                        IResponseHandler handler = callbacks.get(cmdId);
                        callbacks.remove(cmdId);
                        if (handler != null) {
                            response.remove(Constants.TRANSMIT_ID);
                            handler.handle(response);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        stdOutReaderThread.start();
        // input
        stdin = new DataOutputStream(process.getOutputStream());
        stdin.writeBytes(String.format("app_process -Djava.class.path=%s /system/bin %s\n", classPath, entryCls));
    }
    public void send(String message) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        _sendDataToAppProcess(payload);
    }
    public void send(JsonObject payload) throws IOException {
        _sendDataToAppProcess(payload);
    }
    public void send(JsonObject payload, IResponseHandler callback) throws IOException {
        String transmitId = UUID.randomUUID().toString();
        callbacks.put(transmitId, callback);
        payload.addProperty(Constants.TRANSMIT_ID, transmitId);
        _sendDataToAppProcess(payload);
    }
    public void terminate() throws IOException {
        JsonObject endSignal = new JsonObject();
        endSignal.addProperty(Constants.END_SIGNAL, "");
        send(endSignal, (res) -> {
            if (stdOutReaderThread != null && !stdOutReaderThread.isInterrupted()) {
                stdOutReaderThread.interrupt();
                stdOutReaderThread = null;
            }
            stdout = null;
            stdin = null;
            process.destroy();
            process = null;
        });
    }

    private void _sendDataToAppProcess(JsonObject data) throws IOException {
        String encodedData = StringHelper._encode(data.toString()) + "\n";
        stdin.writeBytes(encodedData);
    }

    private JsonObject _readDataFromAppProcess() throws IOException {
        String result = StringHelper._decode(stdout.readLine());
        if (result.contains(Constants.TRANSMIT_ID)) {
            return StringHelper.toJsonObject(result);
        }
        return null;
    }
}
