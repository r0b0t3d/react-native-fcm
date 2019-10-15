package com.evollu.react.fcm;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

public class FIRBackgroundMessagingService extends HeadlessJsTaskService {
    @Override
    protected @Nullable
    HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            WritableMap messageMap = Arguments.fromBundle(extras);
            return new HeadlessJsTaskConfig(
                    "FIRBackgroundMessage",
                    messageMap,
                    60000,
                    false
            );
        }
        return null;
    }
}
