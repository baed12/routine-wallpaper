package com.aj.routine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wallpaper showing the whole routine in two columns. The block running now
 * is drawn bright and the others dim. Nothing interactive: no music, no
 * buttons. The clock is read every second, so the highlight follows the day.
 */
public class RoutineWallpaperService extends WallpaperService {

    private static final long FRAME_MS = 1000L;        // highlight changes at most once a second
    private static final long RELOAD_MS = 5 * 60_000L; // re-read the calendar every 5 minutes

    @Override
    public Engine onCreateEngine() {
        return new RoutineEngine();
    }

    private class RoutineEngine extends Engine {

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable frame = this::drawFrame;
        private final Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint taskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        private List<RoutineMath.Block> blocks = new ArrayList<>();
        private long lastLoad = 0L;
        private boolean visible = false;

        RoutineEngine() {
            headerPaint.setFakeBoldText(true);
            headerPaint.setTextAlign(Paint.Align.LEFT);
            taskPaint.setTextAlign(Paint.Align.LEFT);
            messagePaint.setColor(Color.rgb(170, 170, 180));
            messagePaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            if (visible) {
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

        private void reloadIfStale() {
            long now = System.currentTimeMillis();
            if (now - lastLoad > RELOAD_MS) {
                blocks = CalendarReader.readRoutineBlocks(RoutineWallpaperService.this, now);
                lastLoad = now;
            }
        }

        private void drawFrame() {
            handler.removeCallbacks(frame);
            reloadIfStale();

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

            if (!CalendarReader.hasPermission(RoutineWallpaperService.this)) {
                drawMessage(canvas, w, h, "Open Routine Wallpaper to allow calendar access.");
                return;
            }
            if (blocks.isEmpty()) {
                drawMessage(canvas, w, h, "No routine today.");
                return;
            }

            float headerSize = w * 0.042f;
            float taskSize = w * 0.032f;
            float lineGap = taskSize * 1.6f;
            headerPaint.setTextSize(headerSize);
            taskPaint.setTextSize(taskSize);

            int current = RoutineMath.currentBlockIndex(blocks, now);
            List<List<RoutineMath.Block>> columns = RoutineMath.columns(blocks);

            float top = h * 0.30f;                 // keep the lock-screen clock area clear
            float bottom = h * 0.92f;
            float[] columnX = {w * 0.07f, w * 0.53f};

            for (int c = 0; c < columns.size(); c++) {
                float y = top;
                for (RoutineMath.Block b : columns.get(c)) {
                    boolean running = blocks.indexOf(b) == current;
                    int alpha = running ? 255 : 110;

                    headerPaint.setColor(Color.WHITE);
                    headerPaint.setAlpha(alpha);
                    canvas.drawText(b.title + "  " + timeFormat.format(b.start),
                            columnX[c], y, headerPaint);
                    y += lineGap * 1.2f;

                    taskPaint.setColor(Color.WHITE);
                    taskPaint.setAlpha(alpha);
                    for (String task : b.tasks) {
                        if (y > bottom) break;
                        canvas.drawText("• " + task, columnX[c] + w * 0.02f, y, taskPaint);
                        y += lineGap;
                    }
                    y += lineGap * 0.8f;
                    if (y > bottom) break;
                }
            }
        }

        private void drawMessage(Canvas canvas, int w, int h, String text) {
            messagePaint.setTextSize(w * 0.04f);
            canvas.drawText(text, w / 2f, h / 2f, messagePaint);
        }
    }
}
