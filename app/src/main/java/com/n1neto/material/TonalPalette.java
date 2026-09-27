package com.n1neto.material;

import android.graphics.Color;

/**
 * Tiny Material-style tonal palette generator (Material You-like) used for the
 * "Classic" theme and as a fallback on devices older than Android 12.
 * Implements the HCT/Material color spec's simplified path: sRGB -> CAM16 (XYZ->LMS->MA)
 * with OK-like lightness correction, then generates tonal spots and harmonizes hues.
 * ~3 KB of code instead of shipping the full material-color-utilities library.
 */
public final class TonalPalette {

    // --- CAM16-ish conversion matrices --------------------------------------
    private static final float[] XYZ_TO_LMS = {
            0.401288f, 0.650173f, -0.051461f,
           -0.250868f, 0.981148f, -0.031047f,
           -0.002079f, 0.020663f,  0.955574f };
    private static final float[] LMS_TO_XYZ = {
            1.8620678f, -1.1577666f, 0.2979755f,
            0.4983778f,  0.0076607f, -0.0134475f,
           -0.0450970f,  0.0923105f,  1.5708007f };
    private static final float[] LMS_TO_MA = {
            0.400f, 0.400f, -0.400f,
            1.000f, -2.000f, 1.000f,
            0.050f, 0.050f, 0.0f };
    private static final float[] MA_TO_LMS = {
            5.00526f,   1.34890f,   3.76460f,
            5.00527f,   2.15019f,   3.76471f,
            1.01047f,   0.56794f,   3.72353f };

    private static final double WHITE_X = 0.95047, WHITE_Y = 1.0, WHITE_Z = 1.08883;

    public final float hue;      // degrees
    public final float chroma;   // M*
    public final float lightness; // 0..100

    public TonalPalette(float hue, float chroma, float lightness) {
        this.hue = hue;
        this.chroma = chroma;
        this.lightness = lightness;
    }

    /** Decompose an ARGB color into approximate HCT (hue, chroma, lightness). */
    public static TonalPalette fromColor(int argb) {
        float r = linear(Color.red(argb) / 255f);
        float g = linear(Color.green(argb) / 255f);
        float b = linear(Color.blue(argb) / 255f);
        float X = 0.4123388f * r + 0.3576206f * g + 0.1805187f * b;
        float Y = 0.2126f * r + 0.7152f * g + 0.0722f * b;
        float Z = 0.0193214f * r + 0.1191972f * g + 0.9505279f * b;
        float l = mat(XYZ_TO_LMS, 0, X, Y, Z);
        float m = mat(XYZ_TO_LMS, 1, X, Y, Z);
        float s = mat(XYZ_TO_LMS, 2, X, Y, Z);
        float lD = cam16Tone(l), mD = cam16Tone(m), sD = cam16Tone(s);
        float a = mat(LMS_TO_MA, 0, lD, mD, sD);
        float bb = mat(LMS_TO_MA, 1, lD, mD, sD);
        float cC = mat(LMS_TO_MA, 2, lD, mD, sD);
        float hue = (float) Math.toDegrees(Math.atan2((double) bb, (double) a));
        if (hue < 0) hue += 360;
        float chroma = (float) Math.sqrt((double) a * a + (double) bb * bb);
        float lightness = yToLuma((float) (Y / WHITE_Y));
        return new TonalPalette(hue, chroma, lightness);
    }

    /** Build an ARGB color from a hue/chroma/lightness triple (tonal spot). */
    public static int argbFromHct(float hue, float chroma, float luma) {
        // Inverse-HCT approximation: start from the CIE-Lab L (from our perceptual
        // lightness), derive Y, then solve for a*/b* on the hue/chroma circle and
        // convert Lab -> XYZ -> sRGB. This keeps the Material-like tonal behaviour
        // without shipping the full material-color-utilities library.
        double lStar = luma;
        double y;
        if (lStar <= 8.0) {
            y = lStar / 903.3;
        } else {
            double t = (lStar + 16.0) / 116.0;
            y = t * t * t;
        }
        double rad = Math.toRadians(hue);
        double cStar = Math.min((double) chroma, 128.0);
        double aStar = cStar * Math.cos(rad);
        double bStar = cStar * Math.sin(rad);

        double fy = (y > 0.008856451679) ? Math.cbrt(y)
                : (903.2962962 * y + 16.0) / 116.0;
        double fx = (aStar / 500.0) + fy;
        double fz = fy - (bStar / 200.0);

        double xr = (fx > 0.206896552) ? fx * fx * fx : (fx - 0.137931034) / 7.787037037;
        double yr = (fy > 0.206896552) ? fy * fy * fy : (fy - 0.137931034) / 7.787037037;
        double zr = (fz > 0.206896552) ? fz * fz * fz : (fz - 0.137931034) / 7.787037037;

        double X = xr * WHITE_X;
        double Yv = yr * WHITE_Y;
        double Z = zr * WHITE_Z;

        double rl = 3.2406 * X - 1.5372 * Yv - 0.4986 * Z;
        double gl = -0.9689 * X + 1.8758 * Yv + 0.0415 * Z;
        double bl = 0.0557 * X - 0.2040 * Yv + 1.0570 * Z;

        int R = to255(delinear((float) rl));
        int G = to255(delinear((float) gl));
        int B = to255(delinear((float) bl));
        return Color.rgb(R, G, B);
    }

    /** Harmonize two colors by rotating the secondary hue toward the primary. */
    public static int harmonize(int primary, int secondary) {
        TonalPalette p = fromColor(primary);
        TonalPalette s = fromColor(secondary);
        float diff = ((p.hue - s.hue + 540f) % 360f) - 180f;
        float newHue = s.hue + diff * 0.25f;
        return argbFromHct(normalize(newHue), s.chroma, s.lightness);
    }

    public static float normalize(float hue) {
        float h = hue % 360f;
        return h < 0 ? h + 360f : h;
    }

    // --- helpers -------------------------------------------------------------

    private static float mat(float[] m, int row, float x, float y, float z) {
        return m[row * 3] * x + m[row * 3 + 1] * y + m[row * 3 + 2] * z;
    }

    private static float cam16Tone(float v) {
        float a = (float) Math.pow(Math.abs(v) / 300f, 0.42f) * Math.signum(v);
        return 400f * a / (a + 24.0f);
    }

    private static float invCam16Tone(float tone) {
        float a = tone / (2400f - tone);
        return (float) Math.signum(a) * 300f * (float) Math.pow(Math.abs(a), 1f / 0.42f);
    }

    private static float linear(float c) {
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static float delinear(float c) {
        if (Float.isNaN(c)) return 0;
        c = Math.max(-0.003f, Math.min(1.003f, c));
        return c <= 0.0031308f ? c * 12.92f : 1.055f * (float) Math.pow(c, 1 / 2.4) - 0.055f;
    }

    private static int to255(float c) {
        return Math.round(Math.max(0f, Math.min(255f, c * 255f)));
    }

    private static float yToLuma(float y) {
        if (y <= 0.00885645165f) return y / 0.00885645165f * 903.3f;
        return (float) (116.0 * Math.pow(y, 1.0 / 3.0) - 16.0);
    }

    private static float lumaToY(float l) {
        if (l <= 8f) return l / 903.3f * 0.00885645165f;
        double t = (l + 16.0) / 116.0;
        return (float) (t * t * t);
    }
}
