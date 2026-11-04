# Routine Wallpaper

A live wallpaper that shows your daily routine as a scrolling list. The current
task sits in the middle, past tasks drift up, and the next task rises from below.
It reads the routine from the calendar already synced to your phone, so no
taps are needed.

## Status

- Timing logic: tested. 22 checks pass (run with the command at the bottom).
- Android app: written, but NOT yet compiled or run on a phone. The build
  environment used to write it could not download the Android SDK. Expect to
  do one build and install step on your laptop, and report anything that fails.

## Build and install (one time, on a laptop)

1. Install Android Studio (free) from developer.android.com/studio.
2. Open Android Studio, choose Open, and select this `routine-wallpaper` folder.
3. Wait for the progress bar at the bottom to finish. It downloads the SDK automatically.
4. Plug your phone in with USB debugging on, then press the green Run button.
   Or choose Build > Build APK(s), copy the APK to the phone, and open it
   (you may need to allow installs from unknown sources).

## Use

1. Open "Routine Wallpaper" on the phone.
2. Tap "Allow calendar access".
3. Tap "Set as live wallpaper" and confirm.

The app needs the routine events from Google Calendar. They must exist on the
phone's calendar, which happens automatically when the same Google account is
signed in.

## How the display works

- The routine comes from events titled "Routine: <block name>". Each block's
  description lists its tasks as numbered lines.
- Each block's time span is split evenly among its tasks. Example: a 6-hour
  block with 6 tasks gives each task 1 hour.
- The list position is calculated from the clock every 50 ms, so motion is
  smooth and never waits for a tap.
- The calendar is re-read every 5 minutes, so changes to the day's events
  show up without reinstalling.

## Known limits

- **Home and lock screen:** Android live wallpapers apply to both screens. You
  can't set a motion wallpaper for the lock screen alone. Keep the home
  screen mostly covered by widgets, or accept that you'll see it there too.
- **Battery:** the wallpaper redraws about 20 times a second. This is light for
  a static display, but it's the main battery cost. It can be slowed down.
- **Tasks have no fixed times:** tasks in a block are shown in order at even
  spacing. If you want a task to sit at a specific time, it needs its own
  time, which we can add later.
- **Confirming alarms and one-tap done:** not in this version. That's the
  later step.
- **Phone manufacturers:** some (notably Samsung and Xiaomi) kill background
  wallpapers to save power. If the display freezes, turn off battery
  optimization for "Routine Wallpaper".

## Run the timing tests (any computer with Java)

    javac -d out app/src/main/java/com/aj/routine/RoutineMath.java test/com/aj/routine/RoutineMathTest.java
    java -cp out com.aj.routine.RoutineMathTest
