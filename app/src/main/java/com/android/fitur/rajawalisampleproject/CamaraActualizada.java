package com.android.fitur.rajawalisampleproject;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import org.rajawali3d.Object3D;
import org.rajawali3d.cameras.ArcballCamera;
import org.rajawali3d.math.MathUtil;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
//TODO: idea: quitar startOrientation quaternion?
/**
 * Author: Sandra Malpica Mallo
 * Date: 27/07/2015.
 * Class: CamaraActualizada.java
 * Comments: app camera. It displays the 3D OpenGL world created in the application to the
 * device screen. Listens to the events that lead to the spheres rotation. Class extended
 * from Rajawali ArcballCamera and modified
 */
public class CamaraActualizada extends ArcballCamera implements SensorEventListener{

    private Context mContext;                       //app context
    private ScaleGestureDetector mScaleDetector;    //scale gesture detector for zooming
    private View.OnTouchListener mGestureListener;  //gesture detector for touch mode
    private GestureDetector mDetector;              //gesture detector
    private View mView;                             //cameras view
    private boolean mIsRotating;                    //true if the sphere is rotating
    private boolean mIsScaling;                     //true if the sphere is scaling
    private Vector3 mCameraStartPos;                //camera start position (not used)
    private Vector3 mPrevSphereCoord;               //previous sphere coordinates (3D)
    private Vector3 mCurrSphereCoord;               //current sphere coordinates (3D)
    private Vector2 mPrevScreenCoord;               //previous sphere coordinates (2D)
    private Vector2 mCurrScreenCoord;               //current sphere coordinates (2D)
    private Quaternion mStartOrientation;           //start orientation (for a rotation movement)
    private Quaternion mCurrentOrientation;         //current orientation
    private Object3D mEmpty;                        //auxiliary target 3D object used for rotation
    private Object3D mTarget;                       //actual camera target
    private Matrix4 mScratchMatrix;                 //auxiliary scratch matrix
    private Vector3 mScratchVector;                 //auxiliary scratch 3D vector
    private double mStartFOV;                       //start cameras field of view
    private final double gbarridoX = 120;           //degrees to screen touch conversion (X axis)
    private final double gbarridoY = 90;            //degrees to screen touch conversion (Y axis)
    private double gradosxpixelX;                   //degrees to pixel conversion (X axis)
    private double gradosxpixelY;                   //degrees to pixel conversion (Y axis)
    private double xAnterior;                       //last x touched point (screen coordinates)
    private double yAnterior;                       //last y touched point (screen coordinates)
    private double yaw=0,pitch=0,roll=0;            //cameras yaw, pitch and roll used in gyro rotation
    private final int touchMode=0;                  //touch camera mode (rotating with touch events)
    private final int gyroMode=1;                   //gyro camera mode (rotating with devices sensors)
    private int mode;                               //actual mode
    private SensorManager sm;                       //sensor manager
    private boolean medicionInicial=true;           //tells if its the first sensor reading
    private Renderer renderer;

    //class constructor without a target
    public CamaraActualizada(Context context, View view) {
        this(context, view, null);
    }

    //class constructor. initializes basic parameters of the camera
    public CamaraActualizada(Context context, View view, Object3D target) {
        super(context,view,target);
//        renderer=r;
        mContext = context;
        mTarget = target;
        mView = view;
        initialize();
        addListeners();
        Log.e("CAMARA", "inicializada");
        gradosxpixelX = gbarridoX/mLastWidth;
        gradosxpixelY = gbarridoY/mLastHeight;
        mode=touchMode;
        sm=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
    }

    //initializes auxiliary variables
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
    }

    //sets the cameras projection matrix
    @Override
    public void setProjectionMatrix(int width, int height) {
        super.setProjectionMatrix(width, height);
    }

    //maps 2D coordinates(a point the user has touched) to 3D sphere coordinates
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
        }
    }

    /*maps the screen touched point to [-1,1] coordinates.*/
    private void mapToScreen(final float x, final float y, Vector2 out)
    {
        out.setX((2 * x - mLastWidth) / mLastWidth);
        out.setY(-(2 * y - mLastHeight) / mLastHeight);
    }

    //starts the rotation in touch mode
    private void startRotation(final float x, final float y)
    {
        if(mode==touchMode){
            mapToScreen(x, y, mPrevScreenCoord);
            //sets the current rotation coordinates
            mCurrScreenCoord.setAll(mPrevScreenCoord.getX(), mPrevScreenCoord.getY());

            mIsRotating = true;
            this.xAnterior=x;
            this.yAnterior=y;
        }
    }

    //updates the rotation as the user scrolls through the screen
    private void updateRotation(final float x, final float y)
    {
        if(mode==touchMode){
            mapToScreen(x, y, mCurrScreenCoord);
            applyRotation(x,y); //applies the rotation in touch mode
        }else{
            applyRotationG();   //applies the rotation in gyro mode
        }
    }

    //creates a quaternion from yaw, pitch and roll sensor values to rotate the sphere
    private void applyRotationG(){
        if(this.mIsRotating){
            this.mCurrentOrientation.fromEuler(-yaw, -pitch, 0.0);
            this.mCurrentOrientation.normalize();

            this.mEmpty.setOrientation(mCurrentOrientation);
        }
    }

    //creates a quaternion from scrolled vector in the screen to rotate the sphere
    private void applyRotation(float x, float y){
        this.gradosxpixelX = gbarridoX/mLastWidth;
        this.gradosxpixelY = gbarridoY/mLastHeight;
        double gradosX = (x - this.xAnterior)*this.gradosxpixelX; //rotation around Y axis - yaw
        double gradosY = (y - this.yAnterior)*this.gradosxpixelY; //rotation around X axis - pitch
        if(this.mIsRotating) {
            this.mCurrentOrientation.fromEuler(gradosX, gradosY, 0);
            Log.e("NUEVO","x w current pre normalize roll"+mCurrentOrientation.getRoll());
            this.mCurrentOrientation.normalize();
            Log.e("NUEVO","x q current post normalize roll"+mCurrentOrientation.getRoll());
            Quaternion q = new Quaternion(this.mStartOrientation);
            q.multiply(this.mCurrentOrientation);
            this.mEmpty.setOrientation(q);
        }
    }

    //ends the sphere rotation
    private void endRotation()
    {
        mStartOrientation.multiply(mCurrentOrientation);
        Log.e("NUEVAS_PRUEBAS","end Rotation");
    }

    private void applyRotation() {
        if (this.mIsRotating) {
            this.mapToSphere((float) this.mPrevScreenCoord.getX(), (float) this.mPrevScreenCoord.getY(), this.mPrevSphereCoord);
            this.mapToSphere((float) this.mCurrScreenCoord.getX(), (float)this.mCurrScreenCoord.getY(), this.mCurrSphereCoord);
            Vector3 rotationAxis = this.mPrevSphereCoord.clone();
            rotationAxis.cross(this.mCurrSphereCoord);
            rotationAxis.normalize();
            double rotationAngle = Math.acos(Math.min(1.0D, this.mPrevSphereCoord.dot(this.mCurrSphereCoord)));
            this.mCurrentOrientation.fromAngleAxis(rotationAxis, MathUtil.radiansToDegrees(-rotationAngle));
            this.mCurrentOrientation.normalize();
            Quaternion q = new Quaternion(this.mStartOrientation);
            q.multiply(this.mCurrentOrientation);
//            normalizedQuaternion(q);
            this.mEmpty.setOrientation(q);
        }
    }

    public Quaternion normalizedQuaternion(Quaternion quaternion){
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
        return quaternion;
    }

    //gets the camera view matrix to the renderer, inverting the spheres rotation
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
    }

    //sets the camera field of view
    public void setFieldOfView(double fieldOfView) {
        synchronized (mFrustumLock) {
            mStartFOV = fieldOfView;
            super.setFieldOfView(fieldOfView);
        }
    }

    //adds event listeners to the camera
    private void addListeners() {
        //these have to be run in the UI thread as the interact with interface fragments
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDetector = new GestureDetector(mContext, new GestureListener());
                mScaleDetector = new ScaleGestureDetector(mContext, new ScaleListener());
                //sets the ontouchlistener
                mGestureListener = new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        mScaleDetector.onTouchEvent(event);//sees if it is scaling

                        if (!mIsScaling) {
                            //if it isn't, it can be a rotation
                            mDetector.onTouchEvent(event);

                            //when the finger is moved up from the screen
                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                //if the sphere was rotating, rotation is stopped
                                if (mIsRotating && mode==touchMode) {
                                    endRotation();
                                    mIsRotating = false;
                                }else{
                                    //else, the control video view changes its visibility
                                    System.out.println("triger up");
                                    MainActivity.timer.cancel();
                                    if (MainActivity.control.getVisibility() == View.INVISIBLE) {
                                        MainActivity.control.setVisibility(View.VISIBLE);
                                        MainActivity.timer.start(); //timer is restarted
                                    } else {
                                        MainActivity.control.setVisibility(View.INVISIBLE);
                                    }
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

    //sets the camera target (3D object to which the camera is looking at)
    public void setTarget(Object3D target) {
        mTarget = target;
        setLookAt(mTarget.getPosition());
    }

    //returns this camera target object
    public Object3D getTarget() {
        return mTarget;
    }

    //scroll listener
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
            //starts or updates the rotation with the upcoming event x and y screen values
            if(!mIsRotating) {
                startRotation(event2.getX(), event2.getY());
                return false;
            }else{
                mIsRotating = true;
                updateRotation(event2.getX(), event2.getY());
                return false;
            }
        }
    }

    //scale listener
    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        //zooms in or out according to the scale detector value
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            double fov = Math.max(30.0D, Math.min(54.0D, CamaraActualizada.this.mStartFOV * (1.0D / (double)detector.getScaleFactor())));
            setFieldOfView(fov);
            return true;
        }

        //the zoom begins
        @Override
        public boolean onScaleBegin (ScaleGestureDetector detector) {
            mIsScaling = true;
            mIsRotating = false;
            return super.onScaleBegin(detector);
        }

        //the zoom ends
        @Override
        public void onScaleEnd (ScaleGestureDetector detector) {
            mIsRotating = false;
            mIsScaling = false;
        }
    }

    //method called when the registered sensors change
    @Override
    public void onSensorChanged(SensorEvent event){
        // It is good practice to check that we received the proper sensor event
        System.out.println("onSensorChanged");
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
        {
            float[] mRotationMatrix = new float[16];
            float[] mAuxMatrix = new float[16];
            float[] orientationVals = new float[3];


            if(mode!=touchMode && mode!=gyroMode){
//                Quaternion mQuaternion = new Quaternion();
                SensorManager.getQuaternionFromVector(mRotationMatrix, event.values);
                Matrix4 aux= this.getViewMatrix();
//                renderer.setSensorOrientation(aux.getFloatValues());
//                renderer.setCameraOrientation(mCurrentOrientation);
            }



            // Convert the rotation-vector to a 4x4 matrix.
            SensorManager.getRotationMatrixFromVector(mRotationMatrix,
                    event.values);
            SensorManager
                    .remapCoordinateSystem(mRotationMatrix,
                           SensorManager.AXIS_X, SensorManager.AXIS_Z,
                            mAuxMatrix);

            SensorManager.getOrientation(mAuxMatrix, orientationVals);
            // Optionally convert the result from radians to degrees
            orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
            orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
            orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);

            System.out.println(" Yaw: " + orientationVals[0] + " Pitch: "
                    + orientationVals[1] + " Roll (not used): "
                    + orientationVals[2]);

            yaw=orientationVals[0];
            pitch = orientationVals[1];
            roll = orientationVals[2];
            //to know what is the device actual orientation
//            WindowManager windowManager=(WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
//            int orientacion =windowManager.getDefaultDisplay().getRotation();
            applyRotationG();

        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1)
    {
        //Do nothing.
    }

    //changes the camera mode
    public void switchMode(int m){
        this.mode=m;
        Log.e("GYRO","mode switched to " + mode);
        if (mode == gyroMode){
            Log.e("GYRO", "gyro registered");
//            sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
            //correccion del yaw, pitch y roll
//            if(medicionInicial){
//                medicionInicial=false;
//                WindowManager windowManager=(WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
//                int orientacion =windowManager.getDefaultDisplay().getRotation();
//                switch(orientacion){
//                    case Surface.ROTATION_0:
//                        break;
//                    case Surface.ROTATION_90:
//                        break;
//                    case Surface.ROTATION_180:
//                        break;
//                    case Surface.ROTATION_270:
//                        break;
//                }
//            }
            mIsRotating=true;
            mIsScaling=false;
        }else if(mode == touchMode){
            sm.unregisterListener(this, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
            Log.e("GYRO", "gyro unregistered");
            mIsRotating=false;
            medicionInicial=true;
        }else{
//            sm.registerListener(this,sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
        }
    }
}
