package com.android.fitur.rajawalisampleproject;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import org.rajawali3d.Object3D;
import org.rajawali3d.cameras.ArcballCamera;
import org.rajawali3d.math.MathUtil;
import org.rajawali3d.math.Matrix;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

/**
 * Created by Fitur on 27/07/2015.
 */
public class CamaraActualizada extends ArcballCamera{

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

    /******************************/
//    private float mPreviousX;
//    private float mPreviousY;
//
//    private float mDensity;
//    private volatile float mDeltaX;
//    private volatile float mDeltaY;
//    /** Store the accumulated rotation. */
//    private final double[] mAccumulatedRotation = new double[16];
//
//    /** Store the current rotation. */
//    private final double[] mCurrentRotation = new double[16];
    /******************************/

    public CamaraActualizada(Context context, View view) {
        this(context, view, null);
    }

    public CamaraActualizada(Context context, View view, Object3D target) {
        super(context,view,target);
        mContext = context;
        mTarget = target;
        mView = view;
        initialize();
        addListeners();
        Log.e("CAMARA","inicializada");
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

    @Override
    public void setProjectionMatrix(int width, int height) {
        super.setProjectionMatrix(width, height);
    }

    private void mapToSphere(final float x, final float y, Vector3 out)
    {
        float lengthSquared = x * x + y * y;
        if (lengthSquared > 1)
        {
            out.setAll(x, y, 0);
            out.normalize();
        }
        else
        {
            out.setAll(x, y, Math.sqrt(1 - lengthSquared));
//            out.setAll(x,y,0);//rota 90,180,270 grados
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

    }

    private void updateRotation(final float x, final float y)
    {
        mapToScreen(x, y, mCurrScreenCoord);
//        Log.e("orig", "x " + x + " y " + y);
//        Log.e("TO_SCREEN", "x "+mCurrScreenCoord.getX()+" y "+mCurrScreenCoord.getY());
//        Log.e("UPD_ROT_COORD","x "+x+" y "+y);
        applyRotation();
    }

    private void endRotation()
    {
        mStartOrientation.multiply(mCurrentOrientation);
    }

    private void applyRotation()
    {
        if(this.mIsRotating) {
            this.mapToSphere((float)this.mPrevScreenCoord.getX(), (float)this.mPrevScreenCoord.getY(), this.mPrevSphereCoord);
            this.mapToSphere((float)this.mCurrScreenCoord.getX(), (float)this.mCurrScreenCoord.getY(), this.mCurrSphereCoord);
            Vector3 rotationAxis = this.mPrevSphereCoord.clone();
            rotationAxis.cross(this.mCurrSphereCoord);
            rotationAxis.normalize();
            double rotationAngle = Math.acos(Math.min(1.0D, this.mPrevSphereCoord.dot(this.mCurrSphereCoord)));
            this.mCurrentOrientation.fromAngleAxis(rotationAxis, MathUtil.radiansToDegrees(-rotationAngle));
            this.mCurrentOrientation.normalize();
            Quaternion q = new Quaternion(this.mStartOrientation);
            q.multiply(this.mCurrentOrientation);
            normalizedQuaternion(q);
            this.mEmpty.setOrientation(q);
        }
//        if (mIsRotating)
//        {
//
//            mapToSphere((float) mPrevScreenCoord.getX(), (float) mPrevScreenCoord.getY(), mPrevSphereCoord);
//            mapToSphere((float) mCurrScreenCoord.getX(), (float) mCurrScreenCoord.getY(), mCurrSphereCoord);
///*//            Log.e("ESF","esfere coordinates x "+mCurrSphereCoord.x+" y "+mCurrSphereCoord.y+" z "+mCurrSphereCoord.z);
//            Vector3 dist = new Vector3(mCurrSphereCoord.x-mPrevSphereCoord.x,mCurrSphereCoord.y-mPrevSphereCoord.y,mCurrSphereCoord.z-mPrevSphereCoord.z);
//            double R = Math.sqrt(Math.pow(mCurrSphereCoord.x,2)+Math.pow(mCurrSphereCoord.y,2)+Math.pow(mCurrSphereCoord.z,2));
//            Quaternion q=new Quaternion();
////            mEmpty.setOrientation(new Quaternion(1,0,0,0));
////            Log.e("ORI","or antes"+mEmpty.getOrientation(q).x);
////            mEmpty.rotate(Vector3.Axis.Z,Math.acos(Math.min(1,dist.z/R)));
////            mEmpty.rotate(Vector3.Axis.Y,Math.acos(Math.min(1,dist.y / R)));
////            mEmpty.rotate(Vector3.Axis.X, Math.acos(Math.min(1, dist.x / R)));
////            Log.e("ORI", "or despues" + mEmpty.getOrientation(q).x);
//            q = normalizedQuaternion(q);
//            mEmpty.setRotation(Math.acos(dist.x/R),Math.acos(dist.y/R),Math.acos(dist.z/R));
//            dist.normalize();*/
//
//            Vector3 rotationAxis = mPrevSphereCoord.clone();
//            rotationAxis.cross(mCurrSphereCoord);
//            rotationAxis.normalize();
//            double rotationAngle = Math.acos(Math.min(1, mPrevSphereCoord.dot(mCurrSphereCoord)));
////            mCurrentOrientation.fromAngleAxis(rotationAxis, MathUtil.radiansToDegrees(-rotationAngle));
//            mCurrentOrientation.fromAngleAxis(rotationAxis, MathUtil.radiansToDegrees(-rotationAngle));
//            mCurrentOrientation.normalize();
//
//            Quaternion q = new Quaternion(mStartOrientation);
//            q.multiply(mCurrentOrientation);
//
//            q = normalizedQuaternion(q);
//
//            mEmpty.setOrientation(q);
//        }
    }

    public Quaternion normalizedQuaternion(Quaternion quaternion){
        /*double pitch = quaternion.getPitch();
        double roll = quaternion.getRoll();
        double yaw = quaternion.getYaw();
        Log.e("QUAT","roll "+roll );
        Log.e("QUAT","pitch "+pitch);
        Log.e("QUAT","yaw "+yaw );
        *//*if(pitch > MathUtil.HALF_PI){
            pitch = MathUtil.HALF_PI;
        }else if(pitch < -MathUtil.HALF_PI){
            pitch = -MathUtil.HALF_PI;
        }*//*
        Log.e("QUAT1","roll "+roll );
        Log.e("QUAT1","pitch "+pitch);
        Log.e("QUAT1","yaw "+yaw );
        return quaternion.fromEuler(yaw,pitch,roll);*/


        /*double x = quaternion.x;
        double y = quaternion.y;
        double z = quaternion.z;
        double w = quaternion.w;
        double heading = Math.atan2(2*y*w-2*x*z,1-2*Math.pow(y,2)-2*Math.pow(z,2));
        double attitude = Math.asin(2 * (x * y) + 2 * (z * w));
        double bank = Math.atan2(2 * x * w - 2 * y * z, 1 - 2 * Math.pow(x, 2) - 2 * Math.pow(z, 2));

        if(attitude > MathUtil.HALF_PI){
            attitude = MathUtil.HALF_PI;
        }else if(attitude < -MathUtil.HALF_PI){
            attitude = -MathUtil.HALF_PI;
        }
        bank = 0;
        Quaternion q = new Quaternion(w,x,y,z);
        q.fromEuler(heading,attitude,bank);
        quaternion.multiply(q);
//        q.fromEuler(heading,attitude,bank);
        return quaternion;*/


//        Quaternion qt = new Quaternion(), qx =  new Quaternion(), qy =  new Quaternion(), qz =  new Quaternion();
        /*double q1 = quaternion.x;
        double q2 = quaternion.y;
        double q3 = quaternion.z;
        double q0 = quaternion.w;*/
        double x = quaternion.x;
        double y = quaternion.y;
        double z = quaternion.z;
        double w = quaternion.w;
//        double yaw = Math.atan2(2 * y * w + 2 * x * z, 1 - 2 * Math.pow(y, 2) - 2 * Math.pow(x, 2));
//        double pitch = Math.asin(2 * (w*x) - 2 * (z * y));
//        double roll = Math.atan2(2 * z * w + 2 * y * x, 1 - 2 * Math.pow(x, 2) - 2 * Math.pow(z, 2));
        double yaw = Math.atan2(2 * x * w + 2 * y * z, 1 - 2 * Math.pow(x, 2) - 2 * Math.pow(y, 2));
        double pitch = Math.asin(2 * (w*y) - 2 * (z *x));
        double roll = Math.atan2(2 * w * z + 2 * x * y, 1 - 2 * Math.pow(y, 2) - 2 * Math.pow(z, 2));
        Log.e("CALCULADOS","Yaw "+yaw+" pitch "+pitch+" roll"+roll);
        Log.e("DEVUELTOS","Yaw "+quaternion.getYaw()+" pitch "+quaternion.getPitch()+" roll"+quaternion.getRoll());
        //alpha es el eje x.
        /*double alpha = Math.atan2(2*((q0*q1)+(q2*q3)), 1-2*(Math.pow(q1,2)+ Math.pow(q2,2)));
        double beta = Math.asin(-2 * (q0 * q2 - q3 * q1));
        double gamma = Math.atan2(2 * ((q0 * q3) + (q1 * q2)), 1 - 2 * (Math.pow(q2, 2) + Math.pow(q3, 2)));*/
        /*if(gamma > MathUtil.HALF_PI){
            gamma = MathUtil.HALF_PI-0.1;
            Log.e("EH?","cambio hecho+");
        }else if(gamma < -MathUtil.HALF_PI){
            gamma = -MathUtil.HALF_PI + 0.1;
            Log.e("EH?","cambio hecho-");
        }*/
//        gamma=0;
//        Log.e("EH?", "yaw " + yaw + " pitch " + pitch + " roll " + roll);
        /*qx.fromAngleAxis(Vector3.Axis.X,yaw);
        qy.fromAngleAxis(Vector3.Axis.Y,pitch);
        qz.fromAngleAxis(Vector3.Axis.Z,roll);
        qt = qx.multiply(qy);
        qt.multiply(qz);
        quaternion.multiply(qt);*/
        return quaternion;
    }

    @Override
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
////        Log.e("CAMARA","en getviewmatrix");
//        Matrix4 m = super.getModelMatrix();
//
////        Matrix4 m = super.getProjectionMatrix();
//        /********************************************************/
////        super.getModelMatrix(); //ojo!!! por que tengo dos aqui? cual uso, objeto o camara? Objeto de momento
////        double[] mTemporaryMatrix = new double[16];
////        double[] mModel = new double[16];
////        Matrix4 m = super.getViewMatrix();
////        Matrix4 mm = super.getModelMatrix();
////        Matrix4 mModel;
//        /********************************************************/
//
//        //takes to m the target position
//        if(mTarget != null) {
//            /********************************************************/
////            /*miar esto!!*/
//////            Log.e("CAMARA","getview dentro if");
////            mModel = this.mTarget.getModelViewMatrix().getDoubleValues(); //ojo!!! te puede servir!!
//////            mModel = this.mTarget.getModelViewProjectionMatrix().getDoubleValues();
//////            mModel = this.mTarget.getModelMatrix().getDoubleValues();
////            Matrix.setIdentityM(mCurrentRotation,0);
////            Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0f, 1f, 0f); //ojo! esto parece que esta al reves
//////            Log.e("ROTATEM","mDeltaX "+mDeltaX+" mDeltaY "+mDeltaY);
////            Matrix.rotateM(mCurrentRotation, 0, mDeltaY, 1f, 0f, 0f); //ojo! esto parece que esta al reves
////            mDeltaX = 0.0f;
////            mDeltaY = 0.0f;
////            // Multiply the current rotation by the accumulated rotation, and then set the accumulated
////            // rotation to the result.
////            Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
////            System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);
////            // Rotate the cube taking the overall rotation into account.
////            Matrix.multiplyMM(mTemporaryMatrix, 0, mModel, 0, mAccumulatedRotation, 0);
////            System.arraycopy(mTemporaryMatrix, 0, mModel, 0, 16);
////            Matrix.multiplyMM(mTemporaryMatrix, 0, mm.getDoubleValues(), 0, m.getDoubleValues(), 0);
////            double[] aux = new double[16];
////            Matrix.multiplyMM(aux, 0, mModel, 0, mTemporaryMatrix, 0);
////
////
//////            System.arraycopy(mTemporaryMatrix,0,aux,0,16);
////            m = new Matrix4(aux);
////            mScratchVector.setAll(mTarget.getPosition());
////            mScratchVector.inverse();
////
////            mScratchMatrix.identity();
////            mScratchMatrix.translate(mScratchVector);
////            m.multiply(mScratchMatrix);
//////            Log.e("CAMARA", "en getviewmatrix final");
////            return m;
//            /********************************************************/
//            mScratchMatrix.identity();
//            mScratchMatrix.translate(mTarget.getPosition());
//            m.multiply(mScratchMatrix);
//        }
//        //combines it with the orientation needed
//        mScratchMatrix.identity();
//        mScratchMatrix.rotate(mEmpty.getOrientation());
//        m.multiply(mScratchMatrix);
//        //inverts the result
//        if(mTarget != null) {
//            mScratchVector.setAll(mTarget.getPosition());
//            mScratchVector.inverse();
//
//            mScratchMatrix.identity();
//            mScratchMatrix.translate(mScratchVector);
//            m.multiply(mScratchMatrix);
//        }
//
//        return m;
    }

    public void setFieldOfView(double fieldOfView) {
        synchronized (mFrustumLock) {
            mStartFOV = fieldOfView;
            super.setFieldOfView(fieldOfView);
        }
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
            double fov = Math.max(30.0D, Math.min(54.0D, CamaraActualizada.this.mStartFOV * (1.0D / (double)detector.getScaleFactor())));
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

    /*
    *
    * Rotating an object in 3D is a neat way of letting your users interact with the scene, but the
    * math can be tricky to get right. In this article, I’ll take a look at a simple way to rotate
    * an object based on the touch events, and how to work around the main drawback of this method.
    * Simple rotations. This is the easiest way to rotate an object based on touch movement. Here
    * is example pseudocode:
    *
    * Matrix.setIdentity(modelMatrix);
    * ... do other translations here ...
    * Matrix.rotateX(totalYMovement);
    * Matrix.rotateY(totalXMovement);
    *
    * This is done every frame.
    *
    * To rotate an object up or down, we rotate it around the X-axis, and to rotate an object left
    * or right, we rotate it around the Y axis. We could also rotate an object around the Z axis if
    * we wanted it to spin around.
    * How to make the rotation appear relative to the user’s point of view.
    * The main problem with the simple way of rotating is that the object is being rotated in
    * relation to itself, instead of in relation to the user’s point of view. If you rotate left and
    * right from a point of zero rotation, the cube will rotate as you expect, but what if you then
    * rotate it up or down 180 degrees? Trying to rotate the cube left or right will now rotate it
    * in the opposite direction!
    * One easy way to work around this problem is to keep a second matrix around that will store all
    * of the accumulated rotations.
    *
    * Here’s what we need to do:
    * Every frame, calculate the delta between the last position of the pointer, and the current
    * position of the pointer. This delta will be used to rotate our accumulated rotation matrix.
    * Use this matrix to rotate the cube. What this means is that drags left, right, up, and down
    * will always move the cube in the direction that we expect.
    *
    * Android Code
    * The code examples here are written for Android, but can easily be adapted to any platform
    * running OpenGL ES.
    * The code is based on Android Lesson Six: An Introduction to Texture Filtering.
    *
    * In LessonSixGLSurfaceView.java, we declare a few member variables:
    private float mPreviousX; private float mPreviousY; private float mDensity;
    *
    * We will store the previous pointer position each frame, so that we can calculate the relative
    * movement left, right, up, or down. We also store the screen density so that drags across the
    * screen can move the object a consistent amount across devices, regardless of the pixel density.
    *
    * Here’s how to get the pixel density:
    * final DisplayMetrics displayMetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    density = displayMetrics.density;
    *
    * Then we add our touch event handler to our custom GLSurfaceView:
    *

public boolean onTouchEvent(MotionEvent event)
{
    if (event != null)
    {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            if (mRenderer != null)
            {
                float deltaX = (x - mPreviousX) / mDensity / 2f;
                float deltaY = (y - mPreviousY) / mDensity / 2f;

                mRenderer.mDeltaX += deltaX;
                mRenderer.mDeltaY += deltaY;
            }
        }

        mPreviousX = x;
        mPreviousY = y;

        return true;
    }
    else
    {
        return super.onTouchEvent(event);
    }
}
    * Every frame, we compare the current pointer position with the previous, and use that to
    * calculate the delta offset. We then divide that delta offset by the pixel density and a
    * slowing factor of 2.0f to get our final delta values. We apply those directly to the renderer
    * to a couple of public variables that we have also declared as volatile, so that they can be
    * updated between threads.
    * Remember, on Android, the OpenGL renderer runs in a different thread than the UI event handler
    * thread, and there is a slim chance that the other thread fires in-between the X and Y variable
    * assignments (there are also additional points of contention with the += syntax). I have left
    * the code like this to bring up this point; as an exercise for the reader I leave it to you to
    * add synchronized statements around the public variable read and write pairs instead of
    * using volatile variables. First, let’s add a couple of matrices and initialize them:

// Store the accumulated rotation.
    private final float[] mAccumulatedRotation = new float[16];

// Store the current rotation.
    private final float[] mCurrentRotation = new float[16];
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {

        // Initialize the accumulated rotation matrix
        Matrix.setIdentityM(mAccumulatedRotation, 0);
    }
    Here’s what our matrix code looks like in the onDrawFrame method:

// Draw a cube.
// Translate the cube into the screen.
            Matrix.setIdentityM(mModelMatrix, 0);
    Matrix.translateM(mModelMatrix, 0, 0.0f, 0.8f, -3.5f);

// Set a matrix that contains the current rotation.
    Matrix.setIdentityM(mCurrentRotation, 0);
    Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f);
    Matrix.rotateM(mCurrentRotation, 0, mDeltaY, 1.0f, 0.0f, 0.0f);
    mDeltaX = 0.0f;
    mDeltaY = 0.0f;

// Multiply the current rotation by the accumulated rotation, and then set the accumulated
// rotation to the result.
    Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
    System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);

// Rotate the cube taking the overall rotation into account.
    Matrix.multiplyMM(mTemporaryMatrix, 0, mModelMatrix, 0, mAccumulatedRotation, 0);
    System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16);
    First we translate the cube.
    Then we build a matrix that will contain the current amount of rotation, between this frame and the preceding frame.
    We then multiply this matrix with the accumulated rotation, and assign the accumulated rotation to the result. The accumulated rotation contains the result of all of our rotations since the beginning.
    Now that we’ve updated the accumulated rotation matrix with the most recent rotation, we finally rotate the cube by multiplying the model matrix with our rotation matrix, and then we set the model matrix to the result.
    The above code might look a bit confusion due to the placement of the variables, so remember the definitions:

    public static void multiplyMM (float[] result, int resultOffset, float[] lhs, int lhsOffset, float[] rhs, int rhsOffset)

    public static void arraycopy (Object src, int srcPos, Object dst, int dstPos, int length)

    Note the position of source and destination for each method call.

    Trouble spots and pitfalls

    The accumulated matrix should be set to identity once when initialized, and should not be reset to identity each frame.
    Previous pointer positions must also be set on pointer down events, not only on pointer move events.
    Watch the order of parameters, and also watch out for corrupting your matrices. Android’s Matrix.multiplyMM states that “the result element values are undefined if the result elements overlap either the lhs or rhs elements.” Use temporary matrices to avoid this problem.
    * */

    /*- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    //como obtiene la x y la y del evento tactil
    if(_isUsingMotion) return;
    UITouch *touch = [touches anyObject];
    float distX = [touch locationInView:touch.view].x -
    [touch previousLocationInView:touch.view].x;
    float distY = [touch locationInView:touch.view].y -
    [touch previousLocationInView:touch.view].y;
    distX *= -0.005;
    distY *= -0.005;
    _fingerRotationX += distY *  _overture / 100;
    _fingerRotationY -= distX *  _overture / 100;

    //como rota en el metodo update
    else
    {
        modelViewMatrix = GLKMatrix4RotateX(modelViewMatrix, _fingerRotationX);
        modelViewMatrix = GLKMatrix4RotateY(modelViewMatrix, _fingerRotationY);
    }
    _modelViewProjectionMatrix = GLKMatrix4Multiply(projectionMatrix, modelViewMatrix);
        */
}
