package com.android.fitur.rajawalisampleproject;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.rajawali3d.Object3D;
import org.rajawali3d.cameras.ArcballCamera;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

import android.app.Activity;
import android.view.MotionEvent;
import org.rajawali3d.math.MathUtil;

/**
 * Created by Fitur on 22/07/2015.
 */
public class mArcballCamera extends ArcballCamera{
    private Context mContext;
    private ScaleGestureDetector mScaleDetector;
    private View.OnTouchListener mGestureListener;
    private GestureDetector mDetector;
    private View mView;
    private boolean mIsRotating;
    private boolean mIsScaling;
    private Vector3 mCameraStartPos;
    private Vector3 mPrevSphereCoord;
    private Vector3 mCurrSphereCoord;
    private Vector2 mPrevScreenCoord;
    private Vector2 mCurrScreenCoord;
    private Quaternion mStartOrientation;
    private Quaternion mCurrentOrientation;
    private Object3D mEmpty;
    private Object3D mTarget;
    private Matrix4 mScratchMatrix;
    private Vector3 mScratchVector;
    private double mStartFOV;

    public mArcballCamera(Context context, View view) {
        this(context, view, (Object3D) null);
    }

    public mArcballCamera(Context context, View view, Object3D target) {
        super(context,view,target);
        this.mContext = context;
        this.mTarget = target;
        this.mView = view;
        this.initialize();
        this.addListeners();
    }

    private void initialize() {
        this.mStartFOV = this.mFieldOfView;
        this.mLookAtEnabled = true;
        this.setLookAt(0.0D, 0.0D, 0.0D);
        this.mEmpty = new Object3D();
        this.mScratchMatrix = new Matrix4();
        this.mScratchVector = new Vector3();
        this.mCameraStartPos = new Vector3();
        this.mPrevSphereCoord = new Vector3();
        this.mCurrSphereCoord = new Vector3();
        this.mPrevScreenCoord = new Vector2();
        this.mCurrScreenCoord = new Vector2();
        this.mStartOrientation = new Quaternion();
        this.mCurrentOrientation = new Quaternion();
    }

    public void setProjectionMatrix(int width, int height) {
        super.setProjectionMatrix(width, height);
    }

    private void mapToSphere(float x, float y, Vector3 out) {
        float lengthSquared = x * x + y * y;
        if(lengthSquared > 1.0F) {
            out.setAll((double)x, (double)y, 0.0D);
            out.normalize();
        } else {
            out.setAll((double)x, (double)y, Math.sqrt((double)(1.0F - lengthSquared)));
        }

    }

    private void mapToScreen(float x, float y, Vector2 out) {
        out.setX((double)((2.0F * x - (float)this.mLastWidth) / (float)this.mLastWidth));
        out.setY((double)(-(2.0F * y - (float)this.mLastHeight) / (float)this.mLastHeight));
    }

    private void startRotation(float x, float y) {
        x=-x;
        this.mapToScreen(x, y, this.mPrevScreenCoord);
        this.mCurrScreenCoord.setAll(this.mPrevScreenCoord.getX(), this.mPrevScreenCoord.getY());
        this.mIsRotating = true;
    }

    private void updateRotation(float x, float y) {
        x=-x;
        this.mapToScreen(x, y, this.mCurrScreenCoord);
        this.applyRotation();
    }

    private void endRotation() {
        this.mStartOrientation.multiply(this.mCurrentOrientation);
    }

    private void applyRotation() {
        if(this.mIsRotating) {
            this.mapToSphere((float) this.mPrevScreenCoord.getX(), (float) this.mPrevScreenCoord.getY(), this.mPrevSphereCoord);
            this.mapToSphere((float) this.mCurrScreenCoord.getX(), (float) this.mCurrScreenCoord.getY(), this.mCurrSphereCoord);
            Vector3 rotationAxis = this.mPrevSphereCoord.clone();
            rotationAxis.cross(this.mCurrSphereCoord);
            rotationAxis.normalize();
            double rotationAngle = Math.acos(Math.min(1.0D, this.mPrevSphereCoord.dot(this.mCurrSphereCoord)));
            this.mCurrentOrientation.fromAngleAxis(rotationAxis, MathUtil.radiansToDegrees(rotationAngle));
            this.mCurrentOrientation.normalize();
            Quaternion q = new Quaternion(this.mStartOrientation);
            q.multiply(this.mCurrentOrientation);
            this.mEmpty.setOrientation(q);
        }

    }

    public Matrix4 getViewMatrix() {
        Matrix4 m = super.getViewMatrix();
        if(this.mTarget != null) {
            this.mScratchMatrix.identity();
            this.mScratchMatrix.translate(this.mTarget.getPosition());
            m.multiply(this.mScratchMatrix);
        }

        this.mScratchMatrix.identity();
        this.mScratchMatrix.rotate(this.mEmpty.getOrientation());
        m.multiply(this.mScratchMatrix);
        if(this.mTarget != null) {
            this.mScratchVector.setAll(this.mTarget.getPosition());
            this.mScratchVector.inverse();
            this.mScratchMatrix.identity();
            this.mScratchMatrix.translate(this.mScratchVector);
            m.multiply(this.mScratchMatrix);
        }

        return m;
    }

    public void setFieldOfView(double fieldOfView) {
        Object var3 = this.mFrustumLock;
        synchronized(this.mFrustumLock) {
            this.mStartFOV = fieldOfView;
            super.setFieldOfView(fieldOfView);
        }
    }

    private void addListeners() {
        ((Activity)this.mContext).runOnUiThread(new Runnable() {
            public void run() {
                mArcballCamera.this.mDetector = new GestureDetector(mArcballCamera.this.mContext, mArcballCamera.this.new GestureListener());
                mArcballCamera.this.mScaleDetector = new ScaleGestureDetector(mArcballCamera.this.mContext, mArcballCamera.this.new ScaleListener());
                mArcballCamera.this.mGestureListener = new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        mArcballCamera.this.mScaleDetector.onTouchEvent(event);
                        if(!mArcballCamera.this.mIsScaling) {
                            mArcballCamera.this.mDetector.onTouchEvent(event);
                            if(event.getAction() == 1 && mArcballCamera.this.mIsRotating) {
                                mArcballCamera.this.endRotation();
                                mArcballCamera.this.mIsRotating = false;
                            }
                        }

                        return true;
                    }
                };
                mArcballCamera.this.mView.setOnTouchListener(mArcballCamera.this.mGestureListener);
            }
        });
    }

    public void setTarget(Object3D target) {
        this.mTarget = target;
        this.setLookAt(this.mTarget.getPosition());
    }

    public Object3D getTarget() {
        return this.mTarget;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private ScaleListener() {
        }

        public boolean onScale(ScaleGestureDetector detector) {
            double fov = Math.max(30.0D, Math.min(100.0D, mArcballCamera.this.mStartFOV * (1.0D / (double)detector.getScaleFactor())));
            mArcballCamera.this.setFieldOfView(fov);
            return true;
        }

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mArcballCamera.this.mIsScaling = true;
            mArcballCamera.this.mIsRotating = false;
            return super.onScaleBegin(detector);
        }

        public void onScaleEnd(ScaleGestureDetector detector) {
            mArcballCamera.this.mIsRotating = false;
            mArcballCamera.this.mIsScaling = false;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private GestureListener() {
        }

        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
            if(!mArcballCamera.this.mIsRotating) {
                mArcballCamera.this.startRotation(event2.getX(), event2.getY());
                return false;
            } else {
                mArcballCamera.this.mIsRotating = true;
                mArcballCamera.this.updateRotation(event2.getX(), event2.getY());
                return false;
            }
        }
    }
}
