/*
 * Do what you want with this file
 */
package miscutils;

import java.awt.Font;

/**
 *
 * @author jstar
 */
public class FontFactory {

    public static Font[] makeFonts(String family, int style, int[] sizes) {
        Font[] fonts = new Font[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            fonts[i] = new Font(family, style, sizes[i]);
        }
        return fonts;
    }

    public static Font[] makeFonts(String family) {
        final int[] sizes = { 12, 18, 24};
        Font[] fonts = new Font[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            fonts[i] = new Font(family, Font.PLAIN, sizes[i]);
        }
        return fonts;
    }
}
