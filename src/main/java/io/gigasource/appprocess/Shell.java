package io.gigasource.appprocess;

import java.io.*;
import java.util.*;

public class Shell {
    public static int executeMode = ShellExecuteMode.AUTO;
    private static boolean rootChecked = false;
    private static boolean rooted = false;
    /**
     * Check root permissions
     *
     * @return true if success
     */
    public static boolean isRooted() {
        if (rootChecked)
            return rooted;

        boolean result = false;
        OutputStream stdin = null;
        InputStream stdout = null;
        try {
            Process process = Runtime.getRuntime().exec("su");
            stdin = process.getOutputStream();
            stdout = process.getInputStream();
            DataOutputStream os = null;
            try {
                os = new DataOutputStream(stdin);
                os.writeBytes("ls /data\n");
                os.writeBytes("exit\n");
                os.flush();
            } catch (IOException ignored) {
            } finally {
                close(os);
            }
            int n = 0;
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(stdout));
                while (reader.readLine() != null) {
                    n++;
                }
            } catch (IOException ignored) {
            } finally {
                close(reader);
            }
            if (n > 0) {
                result = true;
            }
        } catch (IOException ignored) {

        } finally {
            close(stdout);
            close(stdin);
        }

        rootChecked = true;
        rooted = result;
        return result;
    }

    private static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void execute(String cmd) {
        Process p = null;
        DataOutputStream dos;
        String shellCmd;
        if (executeMode == ShellExecuteMode.AUTO) {
            shellCmd = isRooted() ? "su" : "sh";
        } else {
            shellCmd = executeMode == ShellExecuteMode.USER ? "sh" : String.format("su -c \"%s\"", cmd);
        }
        try {
            p = Runtime.getRuntime().exec(shellCmd);
            dos = new DataOutputStream(p.getOutputStream());
            if (executeMode == ShellExecuteMode.AUTO || executeMode == ShellExecuteMode.USER)
                dos.writeBytes(String.format("%s\n", cmd));
            dos.writeBytes("exit\n");
            dos.flush();
            dos.close();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
