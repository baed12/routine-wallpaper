package com.aj.routine;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure timing logic for the routine wallpaper. No Android dependencies,
 * so it can be tested on any JVM.
 *
 * Each block's time span is split evenly among its tasks. The display
 * position is continuous: it moves smoothly between tasks as the clock
 * advances, so the list never jumps and never waits for a tap.
 */
public final class RoutineMath {

    private RoutineMath() {}

    /** One task with its computed time slot. Times are epoch millis. */
    public static final class Task {
        public final String name;
        public final String blockTitle;
        public final long start;
        public final long end;

        public Task(String name, String blockTitle, long start, long end) {
            this.name = name;
            this.blockTitle = blockTitle;
            this.start = start;
            this.end = end;
        }
    }

    /** A block from the calendar: a time span and its ordered task names. */
    public static final class Block {
        public final String title;
        public final long start;
        public final long end;
        public final List<String> tasks;

        public Block(String title, long start, long end, List<String> tasks) {
            this.title = title;
            this.start = start;
            this.end = end;
            this.tasks = tasks;
        }
    }

    /** Splits each block evenly among its tasks and returns them in order. */
    public static List<Task> expand(List<Block> blocks) {
        List<Task> out = new ArrayList<>();
        for (Block b : blocks) {
            int n = b.tasks.size();
            if (n == 0 || b.end <= b.start) continue;
            long span = b.end - b.start;
            for (int i = 0; i < n; i++) {
                long s = b.start + span * i / n;
                long e = b.start + span * (i + 1) / n;
                out.add(new Task(b.tasks.get(i), b.title, s, e));
            }
        }
        return out;
    }

    /**
     * Continuous position of "now" in the task list, as a fractional index.
     * 0.0 = start of first task, 1.0 = start of second task, and so on.
     * Before the first task it goes negative. After the last task ends it
     * holds at tasks.size(), so every task sits above the center and the
     * position stays continuous (no snap back at the end of the day).
     */
    public static double position(List<Task> tasks, long now) {
        if (tasks.isEmpty()) return 0.0;
        Task first = tasks.get(0);
        if (now < first.start) {
            return (double) (now - first.start) / durationOf(first);
        }
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if (now < t.end) {
                return i + (double) (now - t.start) / durationOf(t);
            }
        }
        return tasks.size();
    }

    /**
     * Vertical offset in pixels of each task from the screen center, given
     * the spacing between neighboring tasks. Index k is centered when the
     * position equals k; a task above the center has a negative offset.
     */
    public static double[] offsets(List<Task> tasks, long now, double spacingPx) {
        double pos = position(tasks, now);
        double[] y = new double[tasks.size()];
        for (int k = 0; k < tasks.size(); k++) {
            y[k] = (k - pos) * spacingPx;
        }
        return y;
    }

    /** Index of the block running at "now", or -1 if none is running. */
    public static int currentBlockIndex(List<Block> blocks, long now) {
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            if (now >= b.start && now < b.end) return i;
        }
        return -1;
    }

    /**
     * Splits the blocks into two columns: the first half on the left, the
     * rest on the right. With an odd count, the left column gets the extra.
     */
    public static List<List<Block>> columns(List<Block> blocks) {
        int split = (blocks.size() + 1) / 2;
        List<List<Block>> out = new ArrayList<>();
        out.add(new ArrayList<>(blocks.subList(0, split)));
        out.add(new ArrayList<>(blocks.subList(split, blocks.size())));
        return out;
    }

    private static long durationOf(Task t) {
        return Math.max(1L, t.end - t.start);
    }
}
