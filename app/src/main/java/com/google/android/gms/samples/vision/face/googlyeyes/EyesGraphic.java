/*
 * Copyright (C) The Android Open Source Project
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
package com.google.android.gms.samples.vision.face.googlyeyes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.SystemClock;

import com.google.android.gms.samples.vision.face.googlyeyes.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;

class EyesGraphic extends GraphicOverlay.Graphic {
    private static final float EYE_RADIUS_PROPORTION = 0.45f;
    private static final float IRIS_RADIUS_PROPORTION = EYE_RADIUS_PROPORTION / 2.0f;

    private static final float ID_TEXT_SIZE = 40.0f;
    private static int mCurrentColorIndex = 0;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private Paint mIdPaint;
    private Paint mBoxPaint;
    private Paint mDtPaint;

    private volatile PointF mLeftPosition;
    private volatile boolean mLeftOpen;

    private volatile PointF mRightPosition;
    private volatile boolean mRightOpen;

    private long mLastUpdateTimeMs = SystemClock.elapsedRealtime();
    private long mFirstClosedTimeMs;
    private long mCurrentTimeMs = 0;
    private long mClosedDurationTimeMs = 0;

    private volatile Face mFace;
    private volatile boolean mDrowsiness = false;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };

    EyesGraphic(GraphicOverlay overlay) {
        super(overlay);

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);
        mIdPaint.setTypeface(Typeface.DEFAULT_BOLD);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

        mDtPaint = new Paint();
        mDtPaint.setColor(Color.RED);
        mDtPaint.setTextSize(ID_TEXT_SIZE);
        mIdPaint.setTypeface(Typeface.DEFAULT_BOLD);

    }

    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    void updateEyes(PointF leftPosition, boolean leftOpen,
                    PointF rightPosition, boolean rightOpen, boolean drowsy) {
        mLeftPosition = leftPosition;
        mLeftOpen = leftOpen;

        mRightPosition = rightPosition;
        mRightOpen = rightOpen;
        mDrowsiness = drowsy;

        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        PointF detectLeftPosition = mLeftPosition;
        PointF detectRightPosition = mRightPosition;
        if ((detectLeftPosition == null) || (detectRightPosition == null)) {
            return;
        }

        PointF leftPosition =
                new PointF(translateX(detectLeftPosition.x), translateY(detectLeftPosition.y));
        PointF rightPosition =
                new PointF(translateX(detectRightPosition.x), translateY(detectRightPosition.y));

        float distance = (float) Math.sqrt(
                Math.pow(rightPosition.x - leftPosition.x, 2) +
                        Math.pow(rightPosition.y - leftPosition.y, 2));

        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

        double isDrowsinessProbability = 1.0 - (face.getIsLeftEyeOpenProbability()+face.getIsRightEyeOpenProbability())/2;
        canvas.drawText("drowsiness: " + String.format("%.2f", isDrowsinessProbability), xOffset, yOffset, mIdPaint);
        canvas.drawText("right eye: " + String.format("%.2f", face.getIsRightEyeOpenProbability()), rightPosition.x,rightPosition.y, mIdPaint);
        canvas.drawText("left eye: " + String.format("%.2f", face.getIsLeftEyeOpenProbability()), leftPosition.x, leftPosition.y, mIdPaint);

    }
}