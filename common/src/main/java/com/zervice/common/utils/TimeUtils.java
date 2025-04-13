package com.zervice.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtils {
    public static int DAYS_IN_MONTH = 30;
    public static int DAYS_IN_YEAR = 365;
    public static int HOURS_IN_DAY = 24;
    public static int MINS_IN_HOUR = 60;
    public static int SECS_IN_MIN = 60;
    public static int MILLIS_IN_SEC = 1000;
    public static int SECS_IN_HOUR;
    public static int SECS_IN_DAY;
    static Pattern pattern;


    public static final String PATTERN_DEFAULT = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_TIMESTAMP = "yyyyMMddHHmmssSSS";
    public static final String PATTERN_DAY = "yyyy-MM-dd";
    public static final String PATTERN_DETAILS = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String PATTERN_DAY_CN = "yyyy年MM月dd日";


    private static final String ZONE_ID_GMT_8 = "GMT+8";

    public static boolean isFuture(long millis) {
        return millis > System.currentTimeMillis();
    }

    public static boolean isPast(long millis) {
        return millis < System.currentTimeMillis();
    }

    public static int dayOfMonth() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    public static long toMillis(String str) {
        Matcher matcher = pattern.matcher(str);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid time string with unit");
        } else {
            long result = Long.valueOf(matcher.group(1));
            String var3 = matcher.group(2);
            byte var4 = -1;
            switch(var3.hashCode()) {
                case 100:
                    if (var3.equals("d")) {
                        var4 = 4;
                    }
                    break;
                case 104:
                    if (var3.equals("h")) {
                        var4 = 3;
                    }
                    break;
                case 109:
                    if (var3.equals("m")) {
                        var4 = 2;
                    }
                    break;
                case 115:
                    if (var3.equals("s")) {
                        var4 = 1;
                    }
                    break;
                case 121:
                    if (var3.equals("y")) {
                        var4 = 6;
                    }
                    break;
                case 3490:
                    if (var3.equals("mo")) {
                        var4 = 5;
                    }
                    break;
                case 3494:
                    if (var3.equals("ms")) {
                        var4 = 0;
                    }
            }

            switch(var4) {
                case 0:
                    return result;
                case 1:
                    return result * 1000;
                case 2:
                    return result * 1000 * 60;
                case 3:
                    return result * 1000 * SECS_IN_HOUR;
                case 4:
                    return result * 1000 * SECS_IN_DAY;
                case 5:
                    return result * 1000 * SECS_IN_DAY * DAYS_IN_MONTH;
                case 6:
                    return result * 1000 * SECS_IN_DAY * DAYS_IN_YEAR;
                default:
                    throw new IllegalArgumentException("Invalid time string with unknown unit");
            }
        }
    }

    private TimeUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static {
        SECS_IN_HOUR = MINS_IN_HOUR * SECS_IN_MIN;
        SECS_IN_DAY = HOURS_IN_DAY * SECS_IN_HOUR;
        pattern = Pattern.compile("^(\\d+)(ms|s|m|h|d|mo|y)$");
    }

    public static String format() {
        return format(System.currentTimeMillis(), PATTERN_DEFAULT);
    }

    public static String format(String pattern) {
        return format(System.currentTimeMillis(), pattern);
    }

    public static String format(Long time) {
        return format(time, PATTERN_DEFAULT);
    }

    public static String format(Long time, String pattern) {
        return format(new Date(time), pattern);
    }

    public static String format(Date date, String pattern) {
        SimpleDateFormat format = getFormat(pattern);
        return format.format(date);
    }

    public static Long parse(String timeStr, String pattern) throws ParseException{
        SimpleDateFormat format = getFormat(pattern);
        return format.parse(timeStr).getTime();
    }

    private static SimpleDateFormat getFormat(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setTimeZone(TimeZone.getTimeZone(ZONE_ID_GMT_8));
        return format;
    }
    public static String printTime(long timeInMills) {
        return String.format("GMT=[%s], locale=[%s]", new Date(timeInMills).toGMTString(), new Date(timeInMills).toLocaleString());
    }

    public static long getBeijingTime(String timeStr, String timeFormat) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(timeFormat);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(Constants.ZONE_ID_GMT_8));
        return simpleDateFormat.parse(timeStr).getTime();
    }
    public static long dayBegin(long time) throws Exception{
        String dayStr = format(time, PATTERN_DAY);
        return parse(dayStr, PATTERN_DAY);
    }
}

