package org.vinerdream.petsRevive.utils;

import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static long getCurrentSecond() {
        return System.currentTimeMillis() / 1000;
    }

    public static String formatTime(long seconds) {
        final long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        final long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
        return align(hours) + ":" + align(minutes) + ":" + align(seconds);
    }

    private static String align(long number) {
        String result = String.valueOf(number);
        if (result.length() < 2) {
            result = "0" + result;
        }
        return result;
    }
}
