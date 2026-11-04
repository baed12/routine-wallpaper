package com.aj.routine;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.session.MediaController;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main screen, top to bottom: music controls, the clock, and the routine in
 * two columns with the running block highlighted.
 */
public class MainActivity extends Activity {

    private static final int REQ_CALENDAR = 1;
    private static final long TICK_MS = 1000L;
    private static final long RELOAD_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, TICK_MS);
        }
    };

    private TextView clockView;
    private TextView nowTitle;
    private TextView nowArtist;
    private Button playButton;
    private Button grantMusicButton;
    private LinearLayout routineRow;
    private TextView status;

    private List<RoutineMath.Block> blocks = new ArrayList<>();
    private long lastLoad = 0L;

    private final SimpleDateFormat clockFormat = new SimpleDateFormat("h:mm:ss a", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 11, 18));
        int pad = dp(16);
        root.setPadding(pad, pad * 2, pad, pad);

        // 1. Music, at the top
        LinearLayout music = new LinearLayout(this);
        music.setOrientation(LinearLayout.VERTICAL);
        music.setGravity(Gravity.CENTER);
        music.setBackgroundColor(Color.rgb(24, 24, 36));
        music.setPadding(pad, pad, pad, pad);

        nowTitle = text(18, Color.WHITE, true);
        nowArtist = text(14, Color.rgb(170, 170, 185), false);
        music.addView(nowTitle);
        music.addView(nowArtist);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        Button prev = button("◀◀");
        playButton = button("▶");
        Button next = button("▶▶");
        prev.setOnClickListener(v -> withMusic(MusicController::previous));
        playButton.setOnClickListener(v -> withMusic(MusicController::playPause));
        next.setOnClickListener(v -> withMusic(MusicController::next));
        controls.addView(prev);
        controls.addView(playButton);
        controls.addView(next);
        music.addView(controls);

        grantMusicButton = button("Allow music control");
        grantMusicButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        music.addView(grantMusicButton);
        root.addView(music);

        // 2. The clock
        clockView = text(44, Color.WHITE, true);
        clockView.setGravity(Gravity.CENTER);
        clockView.setPadding(0, dp(20), 0, dp(12));
        root.addView(clockView);

        // 3. The routine, two columns
        routineRow = new LinearLayout(this);
        routineRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(routineRow);

        status = text(14, Color.rgb(170, 170, 185), false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        Button calendar = button("Allow calendar access");
        calendar.setOnClickListener(v ->
                requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, REQ_CALENDAR));
        root.addView(calendar);

        Button set = button("Set as live wallpaper");
        set.setOnClickListener(v -> {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, RoutineWallpaperService.class));
            startActivity(intent);
        });
        root.addView(set);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        lastLoad = 0L; // reload the routine right away
        refresh();
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        clockView.setText(clockFormat.format(now));
        refreshMusic();
        refreshRoutine(now);
    }

    private void refreshMusic() {
        MediaController c = MusicController.find(this);
        if (c == null) {
            nowTitle.setText("No music playing");
            nowArtist.setText("Start a music app to control it here");
            playButton.setText("▶");
            grantMusicButton.setVisibility(View.VISIBLE);
            return;
        }
        grantMusicButton.setVisibility(View.GONE);
        String title = MusicController.title(c);
        String artist = MusicController.artist(c);
        nowTitle.setText(title != null ? title : "Unknown track");
        nowArtist.setText(artist != null ? artist : "");
        playButton.setText(MusicController.isPlaying(c) ? "❚❚" : "▶");
    }

    private void refreshRoutine(long now) {
        if (!CalendarReader.hasPermission(this)) {
            status.setText("Allow calendar access to show your routine.");
            routineRow.removeAllViews();
            return;
        }
        if (now - lastLoad > RELOAD_MS) {
            blocks = CalendarReader.readRoutineBlocks(this, now);
            lastLoad = now;
        }
        if (blocks.isEmpty()) {
            status.setText("No routine events found in your calendar.");
            routineRow.removeAllViews();
            return;
        }
        status.setText("");

        int current = RoutineMath.currentBlockIndex(blocks, now);
        List<List<RoutineMath.Block>> columns = RoutineMath.columns(blocks);

        routineRow.removeAllViews();
        for (List<RoutineMath.Block> column : columns) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp(6), 0, dp(6), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            routineRow.addView(col, lp);

            for (RoutineMath.Block b : column) {
                boolean running = blocks.indexOf(b) == current;
                col.addView(blockView(b, running));
            }
        }
    }

    private LinearLayout blockView(RoutineMath.Block b, boolean running) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackgroundColor(running ? Color.rgb(44, 44, 70) : Color.TRANSPARENT);

        TextView header = text(running ? 17 : 15,
                running ? Color.WHITE : Color.rgb(140, 140, 160), true);
        header.setText(b.title + "  " + timeFormat.format(b.start) + " – " + timeFormat.format(b.end));
        box.addView(header);

        for (String task : b.tasks) {
            TextView t = text(14, running ? Color.WHITE : Color.rgb(120, 120, 140), false);
            t.setText("• " + task);
            t.setPadding(0, dp(3), 0, 0);
            box.addView(t);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        box.setLayoutParams(lp);
        return box;
    }

    private void withMusic(java.util.function.Consumer<MediaController> action) {
        MediaController c = MusicController.find(this);
        if (c != null) action.accept(c);
        refreshMusic();
    }

    private TextView text(int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
