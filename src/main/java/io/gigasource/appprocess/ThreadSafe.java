package io.gigasource.appprocess;

public class ThreadSafe {
  public static void sleep(long ms) {
    try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
  }
}
