package com.aj.routine;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public class RoutineMathTest {

    static int passed = 0, failed = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name + " :: " + detail); }
    }

    static void near(String name, double expected, double actual, double tol) {
        check(name, Math.abs(expected - actual) <= tol,
              "expected " + expected + " got " + actual);
    }

    static long at(String iso) {
        return LocalDateTime.parse(iso).atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
    }

    static List<RoutineMath.Task> sampleDay() {
        // Same blocks as the Google Calendar events created for the 6:30 PM wake time.
        return RoutineMath.expand(Arrays.asList(
            new RoutineMath.Block("Morning", at("2026-11-04T18:30:00"), at("2026-11-05T00:30:00"),
                Arrays.asList("Visualization", "Planning", "Hydrate", "Work", "Bath", "Milk")),
            new RoutineMath.Block("Midday", at("2026-11-05T00:30:00"), at("2026-11-05T03:30:00"),
                Arrays.asList("English post", "Guitar", "Exercise", "Bath", "Walk")),
            new RoutineMath.Block("Evening", at("2026-11-05T03:30:00"), at("2026-11-05T06:30:00"),
                Arrays.asList("Deep voice", "Systems", "Guitar", "Bath")),
            new RoutineMath.Block("Night", at("2026-11-05T06:30:00"), at("2026-11-05T10:00:00"),
                Arrays.asList("Visualization", "Plan vision", "Sleep SOP"))
        ));
    }

    public static void main(String[] args) {
        List<RoutineMath.Task> day = sampleDay();

        check("expand gives 18 tasks in order", day.size() == 18, "size " + day.size());
        check("first task is Visualization", day.get(0).name.equals("Visualization"), day.get(0).name);
        check("tasks are contiguous (each ends where next starts)", contiguous(day), "gap found");
        check("first task starts at wake time", day.get(0).start == at("2026-11-04T18:30:00"), "");
        check("last task ends at night block end", day.get(17).end == at("2026-11-05T10:00:00"), "");
        check("Morning tasks are 1 hour each",
              day.get(0).end - day.get(0).start == 3600_000L, "");
        check("Night tasks are about 1h10m each (3.5h / 3)",
              day.get(16).end - day.get(16).start == 3600_000L * 3.5 / 3, "");

        // Position checks
        near("at wake time position is 0", 0.0,
             RoutineMath.position(day, at("2026-11-04T18:30:00")), 1e-9);
        near("halfway through task 0 is 0.5", 0.5,
             RoutineMath.position(day, at("2026-11-04T19:00:00")), 1e-9);
        near("start of task 1 is 1.0", 1.0,
             RoutineMath.position(day, at("2026-11-04T19:30:00")), 1e-9);
        near("start of Midday block lands on its first task index 6", 6.0,
             RoutineMath.position(day, at("2026-11-05T00:30:00")), 1e-9);

        // Motion is continuous: small clock step gives small position change
        double p1 = RoutineMath.position(day, at("2026-11-04T20:00:00"));
        double p2 = RoutineMath.position(day, at("2026-11-04T20:00:30"));
        check("position moves forward over time", p2 > p1, p1 + " -> " + p2);
        check("30s step moves less than 0.02 task", (p2 - p1) < 0.02, "delta " + (p2 - p1));

        // Monotonic across the whole day, in 1-minute steps
        boolean mono = true;
        double prev = RoutineMath.position(day, at("2026-11-04T17:00:00"));
        for (long t = at("2026-11-04T17:01:00"); t <= at("2026-11-05T11:00:00"); t += 60_000L) {
            double cur = RoutineMath.position(day, t);
            if (cur < prev - 1e-9) { mono = false; break; }
            prev = cur;
        }
        check("position never goes backward across the day", mono, "");

        // Before wake: negative, clamps are sane
        check("before wake position is negative",
              RoutineMath.position(day, at("2026-11-04T18:00:00")) < 0, "");
        // After the day: holds at the end, so all tasks sit above the center
        near("after the routine position holds at end (18)", 18.0,
             RoutineMath.position(day, at("2026-11-05T12:00:00")), 1e-9);

        // Offsets: the task nearest the center is within half a spacing of it
        double[] y = RoutineMath.offsets(day, at("2026-11-05T01:15:00"), 100.0);
        int nearest = (int) Math.round(RoutineMath.position(day, at("2026-11-05T01:15:00")));
        check("nearest task to center is within half a spacing",
              Math.abs(y[nearest]) <= 50.0 + 1e-9, "y " + y[nearest]);
        check("past tasks are above center (negative)", y[0] < 0, "y0 " + y[0]);
        check("future tasks are below center (positive)", y[y.length - 1] > 0, "yN " + y[y.length - 1]);

        // Edge cases
        check("empty list is safe", RoutineMath.position(java.util.Collections.<RoutineMath.Task>emptyList(), 0L) == 0.0, "");
        check("blocks with no tasks are skipped",
              RoutineMath.expand(Arrays.asList(new RoutineMath.Block("X", 0L, 1000L,
                  java.util.Collections.<String>emptyList()))).isEmpty(), "");
        check("zero-length block is skipped",
              RoutineMath.expand(Arrays.asList(new RoutineMath.Block("X", 5L, 5L,
                  Arrays.asList("a")))).isEmpty(), "");

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static boolean contiguous(List<RoutineMath.Task> tasks) {
        for (int i = 1; i < tasks.size(); i++) {
            if (tasks.get(i).start != tasks.get(i - 1).end) return false;
        }
        return true;
    }
}
