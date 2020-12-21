package io.gigasource.appprocess;
import junit.framework.TestCase;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppStarveTest extends TestCase {

  @Test
  public void test1() throws InterruptedException {
    AtomicBoolean died = new AtomicBoolean(false);
    AppStarve appStarve = new AppStarve(500, () -> died.set(true));
    Thread.sleep(1000);
    appStarve.getFeed();
    assertTrue(died.get());
  }

  @Test
  public void test2() throws InterruptedException {
    AtomicBoolean died = new AtomicBoolean(false);
    AppStarve appStarve = new AppStarve(500, () -> died.set(true));
    Thread.sleep(600);
    appStarve.getFeed();
    assertFalse(died.get());
  }
}