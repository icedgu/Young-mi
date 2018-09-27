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

import android.graphics.PointF;
import android.util.Log;

import com.google.android.gms.samples.vision.face.googlyeyes.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.util.HashMap;
import java.util.Map;


class FaceTracker extends Tracker<Face> {
    private static final float EYE_CLOSED_THRESHOLD = 0.4f;

    private GraphicOverlay mOverlay;
    private EyesGraphic mEyesGraphic;
    private UartService mService = null;

    private Map<Integer, PointF> mPreviousProportions = new HashMap<>();

    private boolean mPreviousIsLeftOpen = true;
    private boolean mPreviousIsRightOpen = true;
    private boolean mDrowsiness = false;

    private long mFirstClosedTimeMs;
    private long mCurrentTimeMs = 0;
    private long mClosedDurationTimeMs = 0;


    private CheckStateListener checkStateListener;


    public void setCheckStateListener(CheckStateListener checkStateListener) {
        this.checkStateListener = checkStateListener;
    }

    FaceTracker(GraphicOverlay overlay) {
        mOverlay = overlay;
    }

    @Override
    public void onNewItem(int id, Face face) {
        mEyesGraphic = new EyesGraphic(mOverlay);
        //*--First face recognition point--*//
    }


    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
        mOverlay.add(mEyesGraphic);

        updatePreviousProportions(face);

        PointF leftPosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
        PointF rightPosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);

        float leftOpenScore = face.getIsLeftEyeOpenProbability();
        boolean isLeftOpen;
        if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            isLeftOpen = mPreviousIsLeftOpen;
        } else {
            isLeftOpen = (leftOpenScore > EYE_CLOSED_THRESHOLD);
            mPreviousIsLeftOpen = isLeftOpen;
        }

        float rightOpenScore = face.getIsRightEyeOpenProbability();
        boolean isRightOpen;
        if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            isRightOpen = mPreviousIsRightOpen;
        } else {
            isRightOpen = (rightOpenScore > EYE_CLOSED_THRESHOLD);
            mPreviousIsRightOpen = isRightOpen;
        }

        mEyesGraphic.updateEyes(leftPosition, isLeftOpen, rightPosition, isRightOpen, mDrowsiness);
        mEyesGraphic.updateFace(face);

        if (!isLeftOpen && !isRightOpen) {
            mCurrentTimeMs = 0;
            mClosedDurationTimeMs = 0;
            mFirstClosedTimeMs = System.nanoTime();

            Log.d("TRACK", "shut");

            while (!isLeftOpen && !isRightOpen) {
                mCurrentTimeMs = System.nanoTime();
                mClosedDurationTimeMs = mCurrentTimeMs - mFirstClosedTimeMs;

                //*--drowsiness detect--*//
                if (mClosedDurationTimeMs / 1000000000 >= 3) {
                    //mDrowsiness = true;
                    Log.d("TRACK", "detect");
                    mEyesGraphic.updateEyes(leftPosition, isLeftOpen, rightPosition, isRightOpen, true);
                    mEyesGraphic.updateFace(face);
                    checkStateListener.checkState();
                    //여기다가 text view로 졸음이라고 쓰고 sleep 잠깐 준 다음에 텍뷰 클리어 하면 될 거 같다
                    //Toast "졸음 감지" & Beep 출력!!!!!!!!!!!!!!!!!
                    break;
                }
            }
        }
    }


    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        mOverlay.remove(mEyesGraphic);
    }

    @Override
    public void onDone() {
        mOverlay.remove(mEyesGraphic);
    }

    public static String ServiceChoice(String str) {
        String Vibrate = "55020208F4";
        if (str.equals("vib"))
            return Vibrate;
        else
            return "0";
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private void updatePreviousProportions(Face face) {
        for (Landmark landmark : face.getLandmarks()) {
            PointF position = landmark.getPosition();
            float xProp = (position.x - face.getPosition().x) / face.getWidth();
            float yProp = (position.y - face.getPosition().y) / face.getHeight();
            mPreviousProportions.put(landmark.getType(), new PointF(xProp, yProp));
        }
    }

    private PointF getLandmarkPosition(Face face, int landmarkId) {
        for (Landmark landmark : face.getLandmarks()) {
            if (landmark.getType() == landmarkId) {
                return landmark.getPosition();
            }
        }

        PointF prop = mPreviousProportions.get(landmarkId);
        if (prop == null) {
            return null;
        }

        float x = face.getPosition().x + (prop.x * face.getWidth());
        float y = face.getPosition().y + (prop.y * face.getHeight());
        return new PointF(x, y);
    }

    interface CheckStateListener {
        public void checkState();
    }
}