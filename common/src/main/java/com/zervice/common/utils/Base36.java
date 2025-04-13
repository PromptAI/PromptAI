package com.zervice.common.utils;

import static java.lang.Character.MAX_RADIX;

public class Base36 {
    private static final char[] digitsChar = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int BASE = digitsChar.length;
    private static final int FAST_SIZE = 'z';
    private static final int[] digitsIndex = new int[FAST_SIZE + 1];

    static {
        for (int i = 0; i < FAST_SIZE; i++) {
            digitsIndex[i] = -1;
        }
        for (int i = 0; i < BASE; i++) {
            digitsIndex[digitsChar[i]] = i;
        }
    }

    /**
     * in house impl of decode.
     */
    public static long toLong(String s) {
        long result = 0L;
        long multiplier = 1;
        for (int pos = s.length() - 1; pos >= 0; pos--) {
            int index = getIndex(s, pos);
            result += index * multiplier;
            multiplier *= BASE;
        }
        return result;
    }

    /**
     * in house impl of encode.
     */
    public static String toString(long number) {
        if (number < 0) throw new IllegalArgumentException("Number(Base36) must be positive: " + number);
        if (number == 0) return "0";
        StringBuilder buf = new StringBuilder();
        while (number != 0) {
            buf.append(digitsChar[(int) (number % BASE)]);
            number /= BASE;
        }
        return buf.reverse().toString();
    }

    public static String encode(long number) {
        return Long.toString(number, MAX_RADIX);
    }

    public static long decode(String number) {
        return Long.parseLong(number, MAX_RADIX);   // propagate NumberFormatException
    }

    private static int getIndex(String s, int pos) {
        char c = s.charAt(pos);
        if (c > FAST_SIZE) {
            throw new IllegalArgumentException("Unknown character for Base36: " + s);
        }
        int index = digitsIndex[c];
        if (index == -1) {
            throw new IllegalArgumentException("Unknown character for Base36: " + s);
        }
        return index;
    }

    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            args = new String[]{"1"};
        }

        for(String arg : args) {
            long val = Long.valueOf(arg);
            System.out.println("Base36 encoding [" + arg + "] => " + encode(val));
        }
    }
}
