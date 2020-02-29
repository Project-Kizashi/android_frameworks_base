/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm.utils;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;

import java.util.Arrays;


/** Helper functions for the {@link com.android.server.wm.ScreenRotationAnimation} class*/
public class RotationAnimationUtils {

    /**
     * Converts the provided {@link GraphicBuffer} and converts it to a bitmap to then sample the
     * luminance at the borders of the bitmap
     * @return the average luminance of all the pixels at the borders of the bitmap
     */
    public static float getMedianBorderLuma(GraphicBuffer graphicBuffer, ColorSpace colorSpace) {
        Bitmap hwBitmap = Bitmap.wrapHardwareBuffer(graphicBuffer, colorSpace);
        if (hwBitmap == null) {
            return 0;
        }

        Bitmap swaBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false);
        int height = swaBitmap.getHeight();
        int width = swaBitmap.getWidth();
        float[] borderLumas = new float[2 * width + 2 * height];
        int i;
        int index = 0;
        for (i = 0; i < width; i++, index += 2) {
            borderLumas[index] = swaBitmap.getColor(i, 0).luminance();
            borderLumas[index + 1] = swaBitmap.getColor(i, height - 1).luminance();
        }
        for (i = 0; i < height; i++, index += 2) {
            borderLumas[index] = swaBitmap.getColor(0, i).luminance();
            borderLumas[index + 1] = swaBitmap.getColor(width - 1, i).luminance();
        }
        // Oh, is this too simple and inefficient for you?
        // How about implementing a O(n) solution? https://en.wikipedia.org/wiki/Median_of_medians
        Arrays.sort(borderLumas);
        return borderLumas[borderLumas.length / 2];
    }

    /**
     * Gets the average border luma by taking a screenshot of the {@param surfaceControl}.
     * @see #getMedianBorderLuma(GraphicBuffer, ColorSpace)
     */
    public static float getLumaOfSurfaceControl(Display display, SurfaceControl surfaceControl) {
        if (surfaceControl ==  null) {
            return 0;
        }

        Point size = new Point();
        display.getSize(size);
        Rect crop = new Rect(0, 0, size.x, size.y);
        SurfaceControl.ScreenshotGraphicBuffer buffer =
                SurfaceControl.captureLayers(surfaceControl, crop, 1);
        if (buffer == null) {
            return 0;
        }

        return RotationAnimationUtils.getMedianBorderLuma(buffer.getGraphicBuffer(),
                buffer.getColorSpace());
    }

    public static void createRotationMatrix(int rotation, int width, int height, Matrix outMatrix) {
        switch (rotation) {
            case Surface.ROTATION_0:
                outMatrix.reset();
                break;
            case Surface.ROTATION_90:
                outMatrix.setRotate(90, 0, 0);
                outMatrix.postTranslate(height, 0);
                break;
            case Surface.ROTATION_180:
                outMatrix.setRotate(180, 0, 0);
                outMatrix.postTranslate(width, height);
                break;
            case Surface.ROTATION_270:
                outMatrix.setRotate(270, 0, 0);
                outMatrix.postTranslate(0, width);
                break;
        }
    }
}
