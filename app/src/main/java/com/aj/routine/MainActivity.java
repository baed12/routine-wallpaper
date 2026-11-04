package com.aj.routine;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Setup screen: grant calendar access and set the live wallpaper. */
public class MainActivity extends Activity {

    private static final int REQ_CALENDAR = 1;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        root.addView(status);

        Button allow = new Button(this);
        allow.setText("1. Allow calendar access");
        allow.setOnClickListener(v -> requestPermissions(
                new String[]{Manifest.permission.READ_CALENDAR}, REQ_CALENDAR));
        root.addView(allow);

        Button set = new Button(this);
        set.setText("2. Set as live wallpaper");
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
        refreshStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        refreshStatus();
    }

    private void refreshStatus() {
        if (!CalendarReader.hasPermission(this)) {
            status.setText("Calendar access is off. Tap step 1.");
            return;
        }
        long now = System.currentTimeMillis();
        List<RoutineMath.Block> blocks = CalendarReader.readRoutineBlocks(this, now);
        List<RoutineMath.Task> tasks = RoutineMath.expand(blocks);
        if (tasks.isEmpty()) {
            status.setText("No routine events found in your calendar.\n"
                    + "Make sure the Google account's calendar is synced to this phone.");
        } else {
            status.setText("Routine loaded: " + blocks.size() + " blocks, "
                    + tasks.size() + " tasks.\nTap step 2 to set the wallpaper.");
        }
    }
}
