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
        File scanSrcDir = new File(new File(publicSourceDir).getParent());
        String scanSrcDirPath = scanSrcDir.getPath() + "/";
        File[] listOfFiles = scanSrcDir.listFiles();
        List<String> apkFilePaths = new ArrayList<>();
        String libPath = "";
        for (File file : listOfFiles) {
            if (file.getName().endsWith("apk")) {
                apkFilePaths.add(scanSrcDirPath + file.getName());
            } else if (file.getName().equals("lib")) {
                libPath = file.getPath() + (System.getProperty("os.arch").endsWith("64") ? "/arm64" : "/arm");
            }
        }
        return new AppProcessHost(StringHelper.join(":", apkFilePaths), libPath, entryPoint.getName());
    }

    public List<String> getEnv() {
        Map<String, String> map = System.getenv();
        List<String> environment = new ArrayList<>();
        for (String key : map.keySet()) {
            environment.add(key + "=" + map.get(key));
        }
        return environment;
    }


    private AppProcessHost(String classPath, String libPath, String entryCls) throws IOException {
        String shell = "sh";
        try {
            shell = Shell.isRooted() ? "su" : "sh";
        } catch (Exception ignored) {}
        process = Runtime.getRuntime().exec(shell, getEnv().toArray(new String[0]));
        stdout = new DataInputStream(process.getInputStream());
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
        stdin.writeBytes(String.format("app_process -Djava.class.path=%s -Djava.library.path=%s /system/bin %s\n", classPath, libPath, entryCls));
        stdin.flush();
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
    private DataOutputStream getWriter() {
        return stdin;
    }
    private DataInputStream getReader() {
        return stdout;
    }
    private void _sendDataToAppProcess(JsonObject data) throws IOException {
        String encodedData = StringHelper._encode(data.toString()) + "\n";
        DataOutputStream writer = getWriter();
        writer.writeBytes(encodedData);
        writer.flush();
    }
    private JsonObject _readDataFromAppProcess() throws IOException {
        String result = StringHelper._decode(getReader().readLine());
        if (result.contains(Constants.TRANSMIT_ID)) {
            return StringHelper.toJsonObject(result);
        }
        return null;
    }
}
