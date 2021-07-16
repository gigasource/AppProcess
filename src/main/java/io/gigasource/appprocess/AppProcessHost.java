package io.gigasource.appprocess;

import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AppProcessHost {
    public Process process;
    public DataOutputStream stdin;
    public DataInputStream stdout;
    //
    private Thread stdOutReaderThread;
    private final HashMap<String, IResponseHandler> callbacks;
    private Thread _keepAliveThread;

    /**
     * Create AppProcessHost process with specified entry point
     * @param publicSourceDir applicationContext.getApplicationInfo().publicSourceDir
     * @param entryPoint AppProcess entryPoint class (a class which have public static void main(String[] args) and call only AppProcess.start(..) inside it
     */
    public static AppProcessHost create(Class<?> entryPoint, String publicSourceDir) throws Exception {
        return create(entryPoint, publicSourceDir, Constants.PING_INTERVAL_IN_MILLI_SECONDS);
    }

    public static AppProcessHost create(Class<?> entryPoint, String publicSourceDir, int pingIntervalInMs) throws Exception {
        File scanSrcDir = new File(new File(publicSourceDir).getParent());
        String scanSrcDirPath = scanSrcDir.getPath() + "/";
        File[] listOfFiles = scanSrcDir.listFiles();
        List<String> apkFilePaths = new ArrayList<>();
        String libPath = "/system/lib";
        for (File file : listOfFiles) {
            if (file.getName().endsWith("apk")) {
                apkFilePaths.add(scanSrcDirPath + file.getName());
            } else if (file.getName().equals("lib")) {
                libPath += ":" + file.getPath() + (System.getProperty("os.arch").endsWith("64") ? "/arm64" : "/arm");
            }
        }
        return new AppProcessHost(StringHelper.join(":", apkFilePaths), libPath, entryPoint.getName(), pingIntervalInMs);
    }

    public List<String> getEnv() {
        Map<String, String> map = System.getenv();
        List<String> environment = new ArrayList<>();
        for (String key : map.keySet()) {
            environment.add(key + "=" + map.get(key));
        }
        return environment;
    }

    private AppProcessHost(String classPath, String libPath, String entryCls, int pingIntervalInMs) throws IOException {
        String shell = "sh";
        try { shell = Shell.isRooted() ? "su" : "sh"; } catch (Exception ignored) {}
        process = Runtime.getRuntime().exec(shell, getEnv().toArray(new String[0]));
        stdout = new DataInputStream(process.getInputStream());
        stdin = new DataOutputStream(process.getOutputStream());
        //
        callbacks = new HashMap<>();
        stdOutReaderThread = new Thread(() -> {
            while(true) {
                JsonObject response = _readDataFromAppProcess();
                if (response != null && response.has(Constants.TRANSMIT_ID)) {
                    String cmdId = response.get(Constants.TRANSMIT_ID).getAsString();
                    IResponseHandler handler = callbacks.get(cmdId);
                    if (!cmdId.equals(Constants.LOG_ID))
                        callbacks.remove(cmdId);
                    if (handler != null) {
                        response.remove(Constants.TRANSMIT_ID);
                        handler.handle(response); // exception should be handle in user-code
                    }
                }

                ThreadSafe.sleep(200);
            }
        });
        stdOutReaderThread.start();
        //
        try {
            String cmd = String.format("app_process -Djava.class.path=%s -Djava.library.path=%s /system/bin %s\n", classPath, libPath, entryCls);
            stdin.writeBytes(cmd);
            stdin.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        _keepAppProcessAlive(pingIntervalInMs);
    }

    private void _keepAppProcessAlive(int pingIntervalInMs) {
        _keepAliveThread = new Thread(() -> {
            while(true) {
                JsonObject payload = new JsonObject();
                payload.addProperty(Constants.PING_ID, pingIntervalInMs);
                _sendDataToAppProcess(payload);
                ThreadSafe.sleep(pingIntervalInMs);
            }
        });
        _keepAliveThread.start();
    }

    public void send(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        _sendDataToAppProcess(payload);
    }
    public void send(JsonObject payload) {
        _sendDataToAppProcess(payload);
    }
    public void send(JsonObject payload, IResponseHandler sendHandler) {
        String sendId = UUID.randomUUID().toString();
        callbacks.put(sendId, sendHandler);
        payload.addProperty(Constants.TRANSMIT_ID, sendId);
        _sendDataToAppProcess(payload);
    }
    public void terminate() {
        try {
            if (_keepAliveThread != null) {
                _keepAliveThread.interrupt();
            }
            if (stdOutReaderThread != null && !stdOutReaderThread.isInterrupted()) {
                stdOutReaderThread.interrupt();
                stdOutReaderThread = null;
            }
            stdout = null;
            stdin = null;
            process.destroy();
            process = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private DataOutputStream getWriter() {
        return stdin;
    }
    private DataInputStream getReader() {
        return stdout;
    }
    private void _sendDataToAppProcess(JsonObject data) {
        try {
            String encodedData = StringHelper._encode(data.toString()) + "\n";
            DataOutputStream writer = getWriter();
            if (writer != null) {
                writer.writeBytes(encodedData);
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private JsonObject _readDataFromAppProcess() {
        try {
            DataInputStream reader = getReader();
            if (reader != null) {
                String result = StringHelper._decode(reader.readLine());
                if (result.contains(Constants.TRANSMIT_ID)) {
                    return StringHelper.toJsonObject(result);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public void registerLog(IAppProcessLogCallback logCallback) {
        IResponseHandler logHandler = data -> {
            if (data.has(Constants.LOG_CONTENT) && logCallback != null)
                logCallback.call(data.get(Constants.LOG_CONTENT).getAsString());
        };
        callbacks.put(Constants.LOG_ID, logHandler);
    }
}
