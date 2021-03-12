package io.gigasource.appprocess;

import java.util.Date;

public class AppStarve {
  public interface OnDieCallback{
    void die();
  }

  private OnDieCallback _dieCallback;
  private Date _lastFeedTime;

  public AppStarve(int hungryDurationInMs, OnDieCallback dieCallback) {
    _dieCallback = dieCallback;
    _lastFeedTime = new Date();

    new Thread(() -> {
      while(true) {
        Date now = new Date();
        int timePassedInMs = (int)(now.getTime() - _lastFeedTime.getTime());
        if (timePassedInMs > hungryDurationInMs)
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
    _lastFeedTime = new Date();
  }
}
