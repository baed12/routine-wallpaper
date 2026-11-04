package com.aj.routine;

import android.service.notification.NotificationListenerService;

/**
 * Empty listener. Android requires the app to hold notification access before
 * it can see and control the music that is playing. The user grants this once
 * in settings. This class is only the handle for that permission.
 */
public class MusicListenerService extends NotificationListenerService {
}
