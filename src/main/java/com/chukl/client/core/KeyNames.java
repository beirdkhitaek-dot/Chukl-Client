package com.chukl.client.core;

public final class KeyNames {
    private KeyNames() { }

    public static String name(int k) {
        if (k < 0) return "None";
        if (k >= 65 && k <= 90) return String.valueOf((char) k);
        if (k >= 48 && k <= 57) return String.valueOf((char) k);
        if (k >= 290 && k <= 314) return "F" + (k - 289);
        if (k >= 320 && k <= 329) return "NUM" + (k - 320);
        return switch (k) {
            case 32 -> "SPACE";
            case 39 -> "'";
            case 44 -> ",";
            case 45 -> "-";
            case 46 -> ".";
            case 47 -> "/";
            case 59 -> ";";
            case 61 -> "=";
            case 91 -> "[";
            case 92 -> "\\";
            case 93 -> "]";
            case 96 -> "`";
            case 256 -> "ESC";
            case 257 -> "ENTER";
            case 258 -> "TAB";
            case 259 -> "BACKSPACE";
            case 260 -> "INSERT";
            case 261 -> "DELETE";
            case 262 -> "RIGHT";
            case 263 -> "LEFT";
            case 264 -> "DOWN";
            case 265 -> "UP";
            case 266 -> "PGUP";
            case 267 -> "PGDN";
            case 268 -> "HOME";
            case 269 -> "END";
            case 280 -> "CAPS";
            case 340 -> "LSHIFT";
            case 341 -> "LCTRL";
            case 342 -> "LALT";
            case 344 -> "RSHIFT";
            case 345 -> "RCTRL";
            case 346 -> "RALT";
            default -> "KEY" + k;
        };
    }

    /** Returns the GLFW code, or -2 if unknown. */
    public static int parse(String s) {
        for (int k = 0; k <= 348; k++) {
            if (name(k).equalsIgnoreCase(s)) return k;
        }
        return -2;
    }
}
