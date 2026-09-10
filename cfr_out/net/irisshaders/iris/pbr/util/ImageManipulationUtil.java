/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1011
 *  net.minecraft.class_5253$class_8045
 */
package net.irisshaders.iris.pbr.util;

import net.minecraft.class_1011;
import net.minecraft.class_5253;

public class ImageManipulationUtil {
    public static class_1011 scaleNearestNeighbor(class_1011 image, int newWidth, int newHeight) {
        class_1011 scaled = new class_1011(image.method_4318(), newWidth, newHeight, false);
        float xScale = (float)newWidth / (float)image.method_4307();
        float yScale = (float)newHeight / (float)image.method_4323();
        for (int y = 0; y < newHeight; ++y) {
            for (int x = 0; x < newWidth; ++x) {
                float unscaledX = ((float)x + 0.5f) / xScale;
                float unscaledY = ((float)y + 0.5f) / yScale;
                scaled.method_4305(x, y, image.method_4315((int)unscaledX, (int)unscaledY));
            }
        }
        return scaled;
    }

    public static class_1011 scaleBilinear(class_1011 image, int newWidth, int newHeight) {
        class_1011 scaled = new class_1011(image.method_4318(), newWidth, newHeight, false);
        float xScale = (float)newWidth / (float)image.method_4307();
        float yScale = (float)newHeight / (float)image.method_4323();
        for (int y = 0; y < newHeight; ++y) {
            for (int x = 0; x < newWidth; ++x) {
                float unscaledX = ((float)x + 0.5f) / xScale;
                float unscaledY = ((float)y + 0.5f) / yScale;
                int x1 = Math.round(unscaledX);
                int y1 = Math.round(unscaledY);
                int x0 = x1 - 1;
                int y0 = y1 - 1;
                boolean x0valid = true;
                boolean y0valid = true;
                boolean x1valid = true;
                boolean y1valid = true;
                if (x0 < 0) {
                    x0valid = false;
                }
                if (y0 < 0) {
                    y0valid = false;
                }
                if (x1 >= image.method_4307()) {
                    x1valid = false;
                }
                if (y1 >= image.method_4323()) {
                    y1valid = false;
                }
                int finalColor = 0;
                if (x0valid & y0valid & x1valid & y1valid) {
                    leftWeight = (float)x1 + 0.5f - unscaledX;
                    rightWeight = unscaledX - ((float)x0 + 0.5f);
                    float topWeight = (float)y1 + 0.5f - unscaledY;
                    float bottomWeight = unscaledY - ((float)y0 + 0.5f);
                    float weightTL = leftWeight * topWeight;
                    float weightTR = rightWeight * topWeight;
                    float weightBL = leftWeight * bottomWeight;
                    float weightBR = rightWeight * bottomWeight;
                    int colorTL = image.method_4315(x0, y0);
                    int colorTR = image.method_4315(x1, y0);
                    int colorBL = image.method_4315(x0, y1);
                    int colorBR = image.method_4315(x1, y1);
                    finalColor = ImageManipulationUtil.blendColor(colorTL, colorTR, colorBL, colorBR, weightTL, weightTR, weightBL, weightBR);
                } else if (x0valid & x1valid) {
                    leftWeight = (float)x1 + 0.5f - unscaledX;
                    rightWeight = unscaledX - ((float)x0 + 0.5f);
                    int validY = y0valid ? y0 : y1;
                    int colorLeft = image.method_4315(x0, validY);
                    int colorRight = image.method_4315(x1, validY);
                    finalColor = ImageManipulationUtil.blendColor(colorLeft, colorRight, leftWeight, rightWeight);
                } else if (y0valid & y1valid) {
                    float topWeight = (float)y1 + 0.5f - unscaledY;
                    float bottomWeight = unscaledY - ((float)y0 + 0.5f);
                    int validX = x0valid ? x0 : x1;
                    int colorTop = image.method_4315(validX, y0);
                    int colorBottom = image.method_4315(validX, y1);
                    finalColor = ImageManipulationUtil.blendColor(colorTop, colorBottom, topWeight, bottomWeight);
                } else {
                    finalColor = image.method_4315(x0valid ? x0 : x1, y0valid ? y0 : y1);
                }
                scaled.method_4305(x, y, finalColor);
            }
        }
        return scaled;
    }

    private static int blendColor(int c0, int c1, int c2, int c3, float w0, float w1, float w2, float w3) {
        return class_5253.class_8045.method_48344((int)ImageManipulationUtil.blendChannel(class_5253.class_8045.method_48342((int)c0), class_5253.class_8045.method_48342((int)c1), class_5253.class_8045.method_48342((int)c2), class_5253.class_8045.method_48342((int)c3), w0, w1, w2, w3), (int)ImageManipulationUtil.blendChannel(class_5253.class_8045.method_48347((int)c0), class_5253.class_8045.method_48347((int)c1), class_5253.class_8045.method_48347((int)c2), class_5253.class_8045.method_48347((int)c3), w0, w1, w2, w3), (int)ImageManipulationUtil.blendChannel(class_5253.class_8045.method_48346((int)c0), class_5253.class_8045.method_48346((int)c1), class_5253.class_8045.method_48346((int)c2), class_5253.class_8045.method_48346((int)c3), w0, w1, w2, w3), (int)ImageManipulationUtil.blendChannel(class_5253.class_8045.method_48345((int)c0), class_5253.class_8045.method_48345((int)c1), class_5253.class_8045.method_48345((int)c2), class_5253.class_8045.method_48345((int)c3), w0, w1, w2, w3));
    }

    private static int blendChannel(int v0, int v1, int v2, int v3, float w0, float w1, float w2, float w3) {
        return Math.round((float)v0 * w0 + (float)v1 * w1 + (float)v2 * w2 + (float)v3 * w3);
    }

    private static int blendColor(int c0, int c1, float w0, float w1) {
        return class_5253.class_8045.method_48344((int)ImageManipulationUtil.blendChannel(class_5253.class_8045.method_48342((int)c0), class_5253.class_8045.method_48342((int)c1), w0, w1), (int)ImageManipulationUtil.blendChannel(class_5253.class_8045.method_48347((int)c0), class_5253.class_8045.method_48347((int)c1), w0, w1), (int)ImageManipulationUtil.blendChannel(class_5253.class_8045.method_48346((int)c0), class_5253.class_8045.method_48346((int)c1), w0, w1), (int)ImageManipulationUtil.blendChannel(class_5253.class_8045.method_48345((int)c0), class_5253.class_8045.method_48345((int)c1), w0, w1));
    }

    private static int blendChannel(int v0, int v1, float w0, float w1) {
        return Math.round((float)v0 * w0 + (float)v1 * w1);
    }
}

