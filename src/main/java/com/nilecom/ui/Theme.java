package com.nilecom.ui;

import java.awt.Color;

/**
 * Semantic design tokens with light + dark palettes. The mill3.studio look:
 * warm paper canvas, hard ink borders, single acid-lime accent. Switching
 * {@link #toggle()} flips every token at once so the whole UI restyles on
 * repaint.
 */
public final class Theme {

    private static boolean dark = false;

    private Theme() {}

    public static boolean isDark() { return dark; }
    public static void toggle() { dark = !dark; }
    public static void set(boolean d) { dark = d; }

    /* page + surfaces */
    public static Color canvas()   { return dark ? new Color(0x12, 0x10, 0x0F) : new Color(0xF3, 0xF2, 0xEF); }
    public static Color surface()  { return dark ? new Color(0x1C, 0x19, 0x16) : new Color(0xFB, 0xFA, 0xF7); }
    public static Color fieldBg()  { return dark ? new Color(0x24, 0x21, 0x1C) : new Color(0xFF, 0xFF, 0xFF); }

    /* text */
    public static Color text()     { return dark ? new Color(0xF2, 0xEE, 0xE6) : new Color(0x0A, 0x08, 0x08); }
    public static Color textSoft() { return dark ? new Color(0xDA, 0xD4, 0xC9) : new Color(0x14, 0x12, 0x12); }
    public static Color muted()    { return dark ? new Color(0xA3, 0x9E, 0x95) : new Color(0x6A, 0x66, 0x60); }
    public static Color faint()    { return dark ? new Color(0x6E, 0x69, 0x60) : new Color(0x9C, 0x97, 0x8E); }

    /* lines */
    public static Color border()   { return dark ? new Color(0x49, 0x44, 0x3C) : new Color(0x0A, 0x08, 0x08); }
    public static Color hairline() { return dark ? new Color(0x2C, 0x29, 0x23) : new Color(0xDD, 0xD8, 0xCE); }

    /* strong dark block (header bar, primary fills, badges) — stays dark in both modes */
    public static Color inkBlock() { return dark ? new Color(0x05, 0x04, 0x04) : new Color(0x0A, 0x08, 0x08); }
    public static Color onInk()    { return dark ? new Color(0xF2, 0xEE, 0xE6) : new Color(0xF3, 0xF2, 0xEF); }

    /* accent — constant across modes */
    public static Color accent()    { return new Color(0xC0, 0xFF, 0x0D); }
    public static Color accentDk()  { return new Color(0xAD, 0xE6, 0x00); }
    public static Color onAccent()  { return new Color(0x0A, 0x08, 0x08); }

    /* semantic */
    public static Color danger()  { return new Color(0xFF, 0x68, 0x2C); }
    public static Color success() { return dark ? new Color(0x9C, 0xE8, 0x6A) : new Color(0x2E, 0x7D, 0x32); }
}
