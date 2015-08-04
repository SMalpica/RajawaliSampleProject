package com.android.fitur.rajawalisampleproject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.VideoView;

import org.rajawali3d.Object3D;
import org.rajawali3d.cameras.Camera2D;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

import java.lang.annotation.Target;

/**
 * Created by Fitur on 03/08/2015.
 */
public class Cam2d extends Camera2D {
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
    private final double gbarridoX = 120;
    private final double gbarridoY = 90;
    private double gradosxpixelX;
    private double gradosxpixelY;
    private double giradoenX = 0;
    private double giradoenY = 0;
    private double xAnterior;
    private double yAnterior;

    public Cam2d(Context context, View view, Object3D target){
        super();
        mContext = context;
        mTarget = target;
        mView = view;
        initialize();
        addListeners();
        Log.e("CAMARA","inicializada");
        gradosxpixelX = gbarridoX/mLastWidth;
        gradosxpixelY = gbarridoY/mLastHeight;
    }

    private void initialize() {
        mStartFOV = mFieldOfView;
        mLookAtEnabled = true;
        setLookAt(0, 0, 0);
        mEmpty = new Object3D();
        mScratchMatrix = new Matrix4();
        mScratchVector = new Vector3();
        mCameraStartPos = new Vector3();
        mPrevSphereCoord = new Vector3();
        mCurrSphereCoord = new Vector3();
        mPrevScreenCoord = new Vector2();
        mCurrScreenCoord = new Vector2();
        mStartOrientation = new Quaternion();
        mCurrentOrientation = new Quaternion();

        /********************************************/
//        final DisplayMetrics displayMetrics = new DisplayMetrics();
//        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
//        wm.getDefaultDisplay().getMetrics(displayMetrics);
//        mDensity = displayMetrics.density;
//        // Initialize the accumulated rotation matrix
//        Matrix.setIdentityM(mAccumulatedRotation, 0);
        /********************************************/
    }

    private void addListeners() {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDetector = new GestureDetector(mContext, new GestureListener());
                mScaleDetector = new ScaleGestureDetector(mContext, new ScaleListener());

                mGestureListener = new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        mScaleDetector.onTouchEvent(event);

                        if (!mIsScaling) {
                            mDetector.onTouchEvent(event);

                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                if (mIsRotating) {
                                    endRotation();
                                    mIsRotating = false;
                                }
                            }
                        }

                        return true;
                    }
                };
                mView.setOnTouchListener(mGestureListener);
            }
        });
    }

    public void setTarget(Object3D target) {
        mTarget = target;
        setLookAt(mTarget.getPosition());
    }

    public Object3D getTarget() {
        return mTarget;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
            /*********************************************/
//            if (event2 != null){
//                float x = event2.getX();
//                float y = event2.getY();
//
//                if (event2.getAction() == MotionEvent.ACTION_MOVE){
//                    if (CamaraActualizada.this != null){
//                        float deltaX = (x - mPreviousX) / mDensity / 2f;
//                        float deltaY = (y - mPreviousY) / mDensity / 2f;
//
//                        CamaraActualizada.this.mDeltaX += deltaX;
//                        CamaraActualizada.this.mDeltaY += deltaY;
//                        CamaraActualizada.this.mDeltaY*=0.0005;
//                        CamaraActualizada.this.mDeltaX*=0.0005;
//                    }
//                }
//
//                mPreviousX = x;
//                mPreviousY = y;
//
//                return true;
//            }
//            return false;
            /*********************************************/
            if(!mIsRotating) {
                startRotation(event2.getX(), event2.getY());
                return false;
            }else{
                mIsRotating = true;
                updateRotation(event2.getX(), event2.getY());
//                Log.e("COORD_GLISTENER","x "+event2.getX()+" y "+event2.getY());
//                Log.e("DIST_GLISTENER"," X "+distanceX+" Y "+distanceY);
                return false;
            }
        }
    }

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            double fov = Math.max(30.0D, Math.min(54.0D, Cam2d.this.mStartFOV * (1.0D / (double)detector.getScaleFactor())));
            setFieldOfView(fov);
            return true;
        }

        @Override
        public boolean onScaleBegin (ScaleGestureDetector detector) {
            mIsScaling = true;
            mIsRotating = false;
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd (ScaleGestureDetector detector) {
            mIsRotating = false;
            mIsScaling = false;
        }
    }

    /*maps the screen touched point to [-1,1] coordinates.*/
    private void mapToScreen(final float x, final float y, Vector2 out)
    {
        out.setX((2 * x - mLastWidth) / mLastWidth);
        out.setY(-(2 * y - mLastHeight) / mLastHeight);
    }

    private void startRotation(final float x, final float y)
    {
        mapToScreen(x, y, mPrevScreenCoord);
        mCurrScreenCoord.setAll(mPrevScreenCoord.getX(), mPrevScreenCoord.getY());

        mIsRotating = true;
        this.xAnterior=x;
        this.yAnterior=y;
    }

    private void updateRotation(final float x, final float y)
    {
        mapToScreen(x, y, mCurrScreenCoord);
//        Log.e("orig", "x " + x + " y " + y);
//        Log.e("TO_SCREEN", "x "+mCurrScreenCoord.getX()+" y "+mCurrScreenCoord.getY());
//        Log.e("UPD_ROT_COORD","x "+x+" y "+y);
//        applyRotation();
        applyRotation(x,y);
    }

    private void applyRotation(float x, float y){
        this.gradosxpixelX = gbarridoX/mLastWidth;
        this.gradosxpixelY = gbarridoY/mLastHeight;
//        Log.e("NUEVO","grados x "+x+" "+ this.xAnterior+"  "+this.gradosxpixelX);
        double gradosX = (x - this.xAnterior)*this.gradosxpixelX; //rotation around Y axis - yaw
        double gradosY = (y - this.yAnterior)*this.gradosxpixelY; //rotation around X axis - pitch
//        Log.e("NUEVO","x "+gradosX+" y "+y);
        if(this.mIsRotating) {
            this.mCurrentOrientation.fromEuler(gradosX, gradosY, 0);
            Log.e("NUEVO", "x w current pre normalize roll" + mCurrentOrientation.getRoll());
            this.mCurrentOrientation.normalize();
            Log.e("NUEVO","x q current post normalize roll"+mCurrentOrientation.getRoll());
            Quaternion q = new Quaternion(this.mStartOrientation);
            q.multiply(this.mCurrentOrientation);
//            normalizedQuaternion(q);
            this.mEmpty.setOrientation(q);
        }
    }

    private void endRotation()
    {
        mStartOrientation.multiply(mCurrentOrientation);
        Log.e("NUEVAS_PRUEBAS","end Rotation");
    }
}
