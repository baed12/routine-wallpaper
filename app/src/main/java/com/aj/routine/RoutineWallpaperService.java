package com.aj.routine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Live wallpaper that draws the day's routine as a list. The current task
 * sits in the middle; past tasks drift up and fade; upcoming tasks rise from
 * below. Motion follows the clock, so no taps are needed.
 */
public class RoutineWallpaperService extends WallpaperService {

    private static final long FRAME_MS = 50L;          // about 20 frames per second
    private static final long RELOAD_MS = 5 * 60_000L; // re-read the calendar every 5 minutes

    @Override
    public Engine onCreateEngine() {
        return new RoutineEngine();
    }

    private class RoutineEngine extends Engine {

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable frame = this::drawFrame;
        private final Paint currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint otherPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private List<RoutineMath.Task> tasks = new ArrayList<>();
        private long lastLoad = 0L;
        private boolean visible = false;

        RoutineEngine() {
            currentPaint.setColor(Color.WHITE);
            currentPaint.setFakeBoldText(true);
            currentPaint.setTextAlign(Paint.Align.CENTER);

            otherPaint.setColor(Color.WHITE);
            otherPaint.setTextAlign(Paint.Align.CENTER);

            messagePaint.setColor(Color.rgb(170, 170, 180));
            messagePaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            if (visible) {
                reloadIfStale(true);
                drawFrame();
            } else {
                handler.removeCallbacks(frame);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            visible = false;
            handler.removeCallbacks(frame);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            visible = false;
            handler.removeCallbacks(frame);
        }

        private void reloadIfStale(boolean force) {
            long now = System.currentTimeMillis();
            if (force || now - lastLoad > RELOAD_MS) {
                List<RoutineMath.Block> blocks = CalendarReader.readRoutineBlocks(
                        RoutineWallpaperService.this, now);
                tasks = RoutineMath.expand(blocks);
                lastLoad = now;
            }
        }

        private void drawFrame() {
            handler.removeCallbacks(frame);
            reloadIfStale(false);

            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) render(canvas, System.currentTimeMillis());
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }

            if (visible) handler.postDelayed(frame, FRAME_MS);
        }

        private void render(Canvas canvas, long now) {
            int w = canvas.getWidth();
            int h = canvas.getHeight();
            canvas.drawColor(Color.BLACK);

            float currentSize = w * 0.11f;
            float otherSize = w * 0.06f;
            currentPaint.setTextSize(currentSize);
            otherPaint.setTextSize(otherSize);
            messagePaint.setTextSize(w * 0.04f);

            if (tasks.isEmpty()) {
                canvas.drawText("No routine today.", w / 2f, h / 2f, messagePaint);
                canvas.drawText("Open Routine Wallpaper to allow calendar access.",
                        w / 2f, h / 2f + w * 0.06f, messagePaint);
                return;
            }

            double spacing = h * 0.10;
            double centerY = h / 2.0;
            double[] offsets = RoutineMath.offsets(tasks, now, spacing);
            int nearest = (int) Math.round(RoutineMath.position(tasks, now));

            for (int k = 0; k < tasks.size(); k++) {
                double y = centerY + offsets[k];
                if (y < -spacing || y > h + spacing) continue;

                boolean isCurrent = (k == nearest);
                double distance = Math.abs(offsets[k]) / spacing;
                int alpha = (int) (255 * clamp(1.0 - distance * 0.30, 0.12, 1.0));

                Paint p = isCurrent ? currentPaint : otherPaint;
                p.setAlpha(alpha);
                canvas.drawText(tasks.get(k).name, w / 2f, (float) y, p);
            }
        }

        private double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }
}
