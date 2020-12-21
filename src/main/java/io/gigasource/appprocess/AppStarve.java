package io.gigasource.appprocess;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AppStarve {
  public interface OnDieCallback{
    void die();
  }

  private OnDieCallback _dieCallback;
  private LocalDateTime _lastFeedTime;

  public AppStarve(int hungryDurationInMs, OnDieCallback dieCallback) {
    _dieCallback = dieCallback;
    _lastFeedTime = LocalDateTime.now();

    new Thread(() -> {
      while(true) {
        LocalDateTime now = LocalDateTime.now();
        if (ChronoUnit.MILLIS.between(_lastFeedTime, now) > hungryDurationInMs)
          if (_dieCallback != null)
            _dieCallback.die();

        try {
          Thread.sleep(hungryDurationInMs);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  public void getFeed() {
    _lastFeedTime = LocalDateTime.now();
  }
}
