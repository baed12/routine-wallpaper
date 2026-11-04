package com.aj.routine;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import java.util.List;

/**
 * Finds the music app that is currently active and reads or controls it
 * (Spotify, YouTube Music, and similar). Works only after notification access
 * is granted to this app.
 */
public final class MusicController {

    private MusicController() {}

    /** The first active media session, or null if none or no access. */
    public static MediaController find(Context ctx) {
        try {
            MediaSessionManager msm =
                    (MediaSessionManager) ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> sessions =
                    msm.getActiveSessions(new ComponentName(ctx, MusicListenerService.class));
            if (sessions != null && !sessions.isEmpty()) return sessions.get(0);
        } catch (SecurityException e) {
            // Notification access not granted yet.
        }
        return null;
    }

    public static boolean isPlaying(MediaController c) {
        PlaybackState s = c.getPlaybackState();
        return s != null && s.getState() == PlaybackState.STATE_PLAYING;
    }

    public static String title(MediaController c) {
        MediaMetadata m = c.getMetadata();
        return m == null ? null : m.getString(MediaMetadata.METADATA_KEY_TITLE);
    }

    public static String artist(MediaController c) {
        MediaMetadata m = c.getMetadata();
        return m == null ? null : m.getString(MediaMetadata.METADATA_KEY_ARTIST);
    }

    public static void playPause(MediaController c) {
        if (isPlaying(c)) c.getTransportControls().pause();
        else c.getTransportControls().play();
    }

    public static void next(MediaController c) {
        c.getTransportControls().skipToNext();
    }

    public static void previous(MediaController c) {
        c.getTransportControls().skipToPrevious();
    }
}
