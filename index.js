import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

const EventEmitter = new NativeEventEmitter(NativeModules.RNFIRMessaging || {});

export const FCMEvent = {
  RefreshToken: 'FCMTokenRefreshed',
  Notification: 'FCMNotificationReceived',
  NotificationOpened: 'FCMNotificationOpened',
  DirectChannelConnectionChanged: 'FCMDirectChannelConnectionChanged'
};

export const RemoteNotificationResult = {
  NewData: 'UIBackgroundFetchResultNewData',
  NoData: 'UIBackgroundFetchResultNoData',
  ResultFailed: 'UIBackgroundFetchResultFailed'
};

export const WillPresentNotificationResult = {
  All: 'UNNotificationPresentationOptionAll',
  None: 'UNNotificationPresentationOptionNone'
};

export const NotificationType = {
  Remote: 'remote_notification',
  NotificationResponse: 'notification_response',
  WillPresent: 'will_present_notification',
  Local: 'local_notification'
};

export const NotificationCategoryOption = {
  CustomDismissAction: 'UNNotificationCategoryOptionCustomDismissAction',
  AllowInCarPlay: 'UNNotificationCategoryOptionAllowInCarPlay',
  PreviewsShowTitle: 'UNNotificationCategoryOptionHiddenPreviewsShowTitle',
  PreviewsShowSubtitle: 'UNNotificationCategoryOptionHiddenPreviewsShowSubtitle',
  None: 'UNNotificationCategoryOptionNone'
};

export const NotificationActionOption = {
  AuthenticationRequired: 'UNNotificationActionOptionAuthenticationRequired',
  Destructive: 'UNNotificationActionOptionDestructive',
  Foreground: 'UNNotificationActionOptionForeground',
  None: 'UNNotificationActionOptionNone',
};

export const NotificationActionType = {
  Default: 'UNNotificationActionTypeDefault',
  TextInput: 'UNNotificationActionTypeTextInput',
};

const RNFIRMessaging = NativeModules.RNFIRMessaging;

const FCM = {};

FCM.getInitialNotification = () => {
  return RNFIRMessaging.getInitialNotification();
};

FCM.enableDirectChannel = () => {
  if (Platform.OS === 'ios') {
    return RNFIRMessaging.enableDirectChannel();
  }
};

FCM.isDirectChannelEstablished = () => {
  return Platform.OS === 'ios' ? RNFIRMessaging.isDirectChannelEstablished() : Promise.resolve(true);
};

FCM.getFCMToken = () => {
  return RNFIRMessaging.getFCMToken();
};

FCM.getEntityFCMToken = () => {
  return RNFIRMessaging.getEntityFCMToken();
}

FCM.deleteEntityFCMToken = () => {
  return RNFIRMessaging.deleteEntityFCMToken();
}

FCM.deleteInstanceId = () =>{
  return RNFIRMessaging.deleteInstanceId();
};

FCM.getAPNSToken = () => {
  if (Platform.OS === 'ios') {
    return RNFIRMessaging.getAPNSToken();
  }
};

FCM.requestPermissions = (options = ['alert', 'sound', 'badge', 'provisional']) => {
  if (Platform.OS === 'ios') {
    return RNFIRMessaging.requestPermissions(options);
  }
  return RNFIRMessaging.requestPermissions();
};

FCM.createNotificationChannel = (channel) => {
  if (Platform.OS === 'android') {
    return RNFIRMessaging.createNotificationChannel(channel);
  }
}

FCM.deleteNotificationChannel = (channel) => {
  if (Platform.OS === 'android') {
    return RNFIRMessaging.deleteNotificationChannel(channel);
  }
}

FCM.presentLocalNotification = (details) => {
  details.id = details.id || new Date().getTime().toString();
  details.local_notification = true;
  RNFIRMessaging.presentLocalNotification(details);
};

FCM.scheduleLocalNotification = function(details) {
  if (!details.id) {
    throw new Error('id is required for scheduled notification');
  }
  details.local_notification = true;
  RNFIRMessaging.scheduleLocalNotification(details);
};

FCM.getScheduledLocalNotifications = function() {
  return RNFIRMessaging.getScheduledLocalNotifications();
};

FCM.cancelLocalNotification = (notificationID) => {
  if (!notificationID) {
    return;
  }
  RNFIRMessaging.cancelLocalNotification(notificationID);
};

FCM.cancelAllLocalNotifications = () => {
  RNFIRMessaging.cancelAllLocalNotifications();
};

FCM.removeDeliveredNotification = (notificationID) => {
  if (!notificationID) {
    return;
  }
  RNFIRMessaging.removeDeliveredNotification(notificationID);
};

FCM.removeAllDeliveredNotifications = () => {
  RNFIRMessaging.removeAllDeliveredNotifications();
};

FCM.setBadgeNumber = (number) => {
  RNFIRMessaging.setBadgeNumber(number);
};

FCM.getBadgeNumber = () => {
  return RNFIRMessaging.getBadgeNumber();
};

function finish(result) {
  if (!this._finishCalled) {
    this._finishCalled = true;
    if (Platform.OS === 'ios' && !this._completionHandlerId) {
      return;
    }
    switch (this.notificationType) {
      case NotificationType.Remote:
        if (Platform.OS !== 'ios') {
          return;
        }
        result = result || RemoteNotificationResult.NoData;
        if (!Object.values(RemoteNotificationResult).includes(result)) {
          throw new Error(`Invalid RemoteNotificationResult, use import {RemoteNotificationResult} from 'react-native-fcm' to avoid typo`);
        }
        RNFIRMessaging.finishRemoteNotification(this._completionHandlerId, result);
        return;
      case NotificationType.NotificationResponse:
        if (Platform.OS !== 'ios') {
          return;
        }
        RNFIRMessaging.finishNotificationResponse(this._completionHandlerId);
        return;
      case NotificationType.WillPresent:
        result = result || (this.show_in_foreground ? WillPresentNotificationResult.All : WillPresentNotificationResult.None);
        if (!Object.values(WillPresentNotificationResult).includes(result)) {
          throw new Error(`Invalid WillPresentNotificationResult, make sure you use import {WillPresentNotificationResult} from 'react-native-fcm' to avoid typo`);
        }
        if (Platform.OS === 'ios') {
          RNFIRMessaging.finishWillPresentNotification(this._completionHandlerId, result);
        } else {
          console.warn("Start present", this);
          this.show_in_foreground = true;
          this.priority = 'max';
          this.vibrate = true;
          FCM.presentLocalNotification(this);
        }
        return;
      default:
        return;
    }
  }
}

FCM.on = (event, callback) => {
  if (!Object.values(FCMEvent).includes(event)) {
    throw new Error(`Invalid FCM event subscription, use import {FCMEvent} from 'react-native-fcm' to avoid typo`);
  };

  function prepareNotification(nativeNotif) {
    if (Platform.OS === 'android') {
      return nativeNotif;
    } else {
      const notificationObj = {};
      const dataObj = {};
      Object.keys(nativeNotif).forEach(notifKey => {
        const notifVal = nativeNotif[notifKey];
        if (notifKey === 'aps') {
          notificationObj.alert = notifVal.alert;
          notificationObj.sound = notifVal.sound;
          notificationObj.badgeCount = notifVal.badge;
          notificationObj.category = notifVal.category;
          notificationObj.contentAvailable = notifVal['content-available'];
          notificationObj.threadID = notifVal['thread-id'];
        } else if (notifKey === 'notificationType') {
          notificationObj.notificationType = notifVal;
        } else if (notifKey === '_completionHandlerId') {
          notificationObj._completionHandlerId = notifVal;
        } else if (notifKey === 'notification-action') {
          notificationObj.notificationAction = notifVal;
        } else if (notifKey !== 'data') {
          dataObj[notifKey] = notifVal;
        } else {
          notificationObj[notifKey] = notifVal;
        }
      });
      if (!notificationObj['data']) {
        notificationObj.data = dataObj;
      }
      return notificationObj;
    }
  }
  if (event === FCMEvent.Notification || event === FCMEvent.NotificationOpened) {
    return EventEmitter.addListener(event, async(data) => {
      const notification = prepareNotification(data);
      notification.finish = finish;
      try {
        await callback(notification);
      } catch (err) {
        console.error('Notification handler err:\n'+err.stack);
        throw err;
      }
      
      if (!notification._finishCalled) {
        notification.finish();
      }
    });
  }
  return EventEmitter.addListener(event, callback);
};

FCM.subscribeToTopic = (topic) => {
  RNFIRMessaging.subscribeToTopic(topic);
};

FCM.unsubscribeFromTopic = (topic) => {
  RNFIRMessaging.unsubscribeFromTopic(topic);
};

FCM.send = (senderId, payload) => {
  RNFIRMessaging.send(senderId, payload);
};

FCM.setNotificationCategories = (categories) => {
  if (Platform.OS === 'ios') {
    RNFIRMessaging.setNotificationCategories(categories);
  }
}

export default FCM;

export {};
