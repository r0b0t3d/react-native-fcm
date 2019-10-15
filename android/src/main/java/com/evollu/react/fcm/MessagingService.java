package com.evollu.react.fcm;

import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MessagingService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Log.d(TAG, "Remote message received");
        RemoteMessage.Notification remoteNotification = message.getNotification();
        Map<String, String> notificationData = message.getData();
        Bundle bundle = new Bundle();
        if (remoteNotification != null) {
            bundle.putString("title", remoteNotification.getTitle());
            bundle.putString("message", remoteNotification.getBody());
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
        // We need to run this on the main thread, as the React code assumes that is true.
        // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
        // "Can't create handler inside thread that has not called Looper.prepare()"
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                // Construct and load our normal React JS code bundle
                ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();
                // If it's constructed, send a notification
                if (context != null) {
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                } else {
                    // Otherwise wait for construction, then send the notification
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        }
                    });
                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                        // Construct it in the background
                        mReactInstanceManager.createReactContextInBackground();
                    }
                }
            }
        });
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

    public void buildLocalNotification(RemoteMessage remoteMessage) {
        if (remoteMessage.getData() == null) {
            return;
        }
        Map<String, String> data = remoteMessage.getData();
        String customNotification = data.get("custom_notification");
        if (customNotification != null) {
            try {
                Bundle bundle = BundleJSONConverter.convertToBundle(new JSONObject(customNotification));
                FIRLocalMessagingHelper helper = new FIRLocalMessagingHelper(this.getApplication());
                helper.sendNotification(bundle);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
}
