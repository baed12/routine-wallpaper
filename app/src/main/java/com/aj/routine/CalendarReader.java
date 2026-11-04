package com.aj.routine;

import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the routine blocks from the phone's calendar. The routine events are
 * the ones created in Google Calendar: titles start with "Routine:" and each
 * description lists its tasks as numbered lines ("1. Visualization").
 *
 * Reads only the calendars already synced to this phone. No network calls.
 */
public final class CalendarReader {

    static final String PREFIX = "Routine:";
    private static final Pattern TASK_LINE = Pattern.compile("^\\s*\\d+\\.\\s*(.+)$");
    private static final long HOUR = 3600_000L;

    private CalendarReader() {}

    public static boolean hasPermission(Context ctx) {
        return ctx.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Blocks whose time span overlaps the window from 24h ago to 48h ahead. */
    public static List<RoutineMath.Block> readRoutineBlocks(Context ctx, long now) {
        List<RoutineMath.Block> out = new ArrayList<>();
        if (!hasPermission(ctx)) return out;

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, now - 24 * HOUR);
        ContentUris.appendId(builder, now + 48 * HOUR);

        String[] cols = {Instances.TITLE, Instances.DESCRIPTION, Instances.BEGIN, Instances.END};
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(builder.build(), cols, null, null,
                    Instances.BEGIN + " ASC");
            if (c == null) return out;
            while (c.moveToNext()) {
                String title = c.getString(0);
                if (title == null || !title.startsWith(PREFIX)) continue;
                String name = title.substring(PREFIX.length()).trim();
                List<String> tasks = parseTasks(c.getString(1));
                out.add(new RoutineMath.Block(name, c.getLong(2), c.getLong(3), tasks));
            }
        } catch (SecurityException e) {
            // Permission was revoked between the check and the query.
            return new ArrayList<>();
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    /** Pulls the numbered task lines out of an event description, in order. */
    static List<String> parseTasks(String description) {
        List<String> tasks = new ArrayList<>();
        if (description == null) return tasks;
        for (String line : description.split("\\r?\\n")) {
            Matcher m = TASK_LINE.matcher(line);
            if (m.matches()) tasks.add(m.group(1).trim());
        }
        return tasks;
    }
}
