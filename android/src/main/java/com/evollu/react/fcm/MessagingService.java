package com.evollu.react.fcm;

import java.util.List;
import java.util.Map;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.common.LifecycleState;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MessagingService";

    @Override
    public void onNewToken(@NonNull String s) {
        Log.d(TAG, "onNewToken event received " + s);

        // Broadcast refreshed token
        Intent i = new Intent("com.evollu.react.fcm.FCMRefreshToken");
        Bundle bundle = new Bundle();
        bundle.putString("token", s);
        i.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Log.d(TAG, "Remote message received");
        RemoteMessage.Notification remoteNotification = message.getNotification();
        Map<String, String> notificationData = message.getData();
        Bundle bundle = new Bundle();
        if (remoteNotification != null) {
            bundle.putString("title", remoteNotification.getTitle());
            bundle.putString("body", remoteNotification.getBody());
            bundle.putString("sound", remoteNotification.getSound());
            bundle.putString("color", remoteNotification.getColor());
        }
        Bundle dataBundle = new Bundle();
        for (Map.Entry<String, String> entry : notificationData.entrySet()) {
            dataBundle.putString(entry.getKey(), entry.getValue());
        }
        bundle.putParcelable("data", dataBundle);
        handleBadge(notificationData);

        final Intent intent = new Intent("com.evollu.react.fcm.ReceiveNotification");
        intent.putExtras(bundle);
        if (remoteNotification != null) {
            intent.putExtra("notificationType", "will_present_notification");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        } else {
            intent.putExtra("notificationType", "remote");
            // It is a data message
            if (isAppInForeground(this.getApplicationContext())) {
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            } else {
                try {
                    // If the app is in the background we send it to the Headless JS Service
                    Intent headlessIntent = new Intent(
                            this.getApplicationContext(),
                            FIRBackgroundMessagingService.class
                    );
                    headlessIntent.putExtras(bundle);
                    ComponentName name = this.getApplicationContext().startService(headlessIntent);
                    if (name != null) {
                        HeadlessJsTaskService.acquireWakeLockNow(this.getApplicationContext());
                    }
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "Background messages will only work if the message priority is set to 'high'", ex);
                }
            }
        }
    }

    public void handleBadge(Map<String, String> data) {
        if (data != null && data.containsKey("badge")) {
            BadgeHelper badgeHelper = new BadgeHelper(this);

            String badgeString = data.get("badge");
            if (badgeString == null) {
                return;
            }
            try {
                int badgeCount = Integer.parseInt(badgeString);
                badgeHelper.setBadgeCount(badgeCount);
            } catch (Exception e) {
                Log.e(TAG, "Badge count needs to be an integer", e);
            }
        }
    }

    /**
     * We need to check if app is in foreground otherwise the app will crash.
     * http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
     *
     * @param context Context
     * @return boolean
     */
    public boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return false;

        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (
                    appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            && appProcess.processName.equals(packageName)
            ) {
                ReactContext reactContext;

                try {
                    reactContext = (ReactContext) context;
                } catch (ClassCastException exception) {
                    // Not react context so default to true
                    return true;
                }

                return reactContext.getLifecycleState() == LifecycleState.RESUMED;
            }
        }

        return false;
    }
}
