package com.linksentinel.link;

public final class Base62 {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private Base62() {
    }

    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be zero or positive");
        }
        if (value == 0) {
            return "0";
        }
        char[] buffer = new char[11];
        int pos = buffer.length;
        long remaining = value;
        while (remaining > 0) {
            buffer[--pos] = ALPHABET[(int) (remaining % 62)];
            remaining /= 62;
        }
        return new String(buffer, pos, buffer.length - pos);
    }
}
