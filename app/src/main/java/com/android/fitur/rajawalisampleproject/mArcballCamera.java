package com.android.fitur.rajawalisampleproject;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.rajawali3d.Object3D;
import org.rajawali3d.cameras.ArcballCamera;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Plane;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

import android.app.Activity;
import android.view.MotionEvent;
import org.rajawali3d.math.MathUtil;

/**
 * Author: Rajawali framework. Modified: Sandra Malpica Mallo
 * Date: 22/07/2015.
 * Class: mArcballCamera.java
 * Comments: arcball camera used to handle motion events as well as rotate the spheric player.
 */
public class mArcballCamera extends ArcballCamera{
    private Context mContext;                       //camera context
    private ScaleGestureDetector mScaleDetector;
    private View.OnTouchListener mGestureListener;
    private GestureDetector mDetector;
    private View mView;                             //camera view
    private boolean mIsRotating;
    private boolean mIsScaling;
    private Vector3 mCameraStartPos;
    private Vector3 mPrevSphereCoord;
    private Vector3 mCurrSphereCoord;
    private Vector2 mPrevScreenCoord;
    private Vector2 mCurrScreenCoord;
    private Quaternion mStartOrientation;
    private Quaternion mCurrentOrientation;
    private Quaternion orientacionAnterior;
    private Object3D mEmpty;
    private Object3D mTarget;                       //camera target object
    private Matrix4 mScratchMatrix;                 //used to retrieve info and for storing mid-results
    private Vector3 mScratchVector;                 //used to retrieve info and for storing mid-results
    private double mStartFOV;
    private float originalX;
    private float originalY;
    private Vector3 xAxis;
    private Vector3 yAxis;
    private Vector3 zAxis;

    /**constructor with no target*/
    public mArcballCamera(Context context, View view) {
        this(context, view, (Object3D) null);
    }

    /**default arcballCamera constructor. sets the principal camera components and
    * adds the camera main listeners*/
    public mArcballCamera(Context context, View view, Object3D target) {
        super(context,view,target);
        this.mContext = context;
        this.mTarget = target;
        this.mView = view;
        this.initialize();
        this.addListeners();
    }

    /**initializes de camera components*/
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
        this.xAxis = new Vector3(1,0,0);
        this.yAxis = new Vector3(0,1,0);
        this.zAxis = new Vector3(0,0,1);
    }

    /**sets the projection matrix*/
    public void setProjectionMatrix(int width, int height) {
        super.setProjectionMatrix(width, height);
    }

    /*with the given x and y coordinates returns a 3D vector with x,y,z sphere coordinates*/
    private void mapToSphere(float x, float y, Vector3 out) {
        float lengthSquared = x * x + y * y;
        if(lengthSquared > 1.0F) {
            out.setAll((double)x, (double)y, 0.0D);
            out.normalize();
        } else {
            out.setAll((double)x, (double)y, Math.sqrt((double)(1.0F - lengthSquared)));
        }

    }

    /**with the given x and y coordinates returns a 2D vector with x,y screen coordinates*/
    private void mapToScreen(float x, float y, Vector2 out) {
        out.setX((double) ((2.0F * x - (float) this.mLastWidth) / (float) this.mLastWidth));
        out.setY((double)(-(2.0F * y - (float)this.mLastHeight) / (float)this.mLastHeight));
    }

    /**maps initial x and y touch event coordinates to <mPrevScreenCoord/> and then copies it to
     * mCurrScreenCoord */
    private void startRotation(float x, float y) {
        this.mapToScreen(x, y, this.mPrevScreenCoord);
        this.mCurrScreenCoord.setAll(this.mPrevScreenCoord.getX(), this.mPrevScreenCoord.getY());
        this.mIsRotating = true;
    }


    /**updates <mCurrScreenCoord/> to new screen mapped x and y and then applies rotation*/
    private void updateRotation(float x, float y) {
        this.mapToScreen(x, y, this.mCurrScreenCoord);
        this.applyRotation();
    }

    /** rotates the sphere? when the rotation move ends it multiplies by the original start orientation
     * accumulates 2 rotations*/
    private void endRotation() {
        this.mStartOrientation.multiply(this.mCurrentOrientation);
    }

    /** applies the rotation to the target object*/
    private void applyRotation() {
        if(this.mIsRotating) {
            //maps to sphere coordinates previous and current position
            this.mapToSphere((float) this.mPrevScreenCoord.getX(), (float) this.mPrevScreenCoord.getY(), this.mPrevSphereCoord);
            this.mapToSphere((float) this.mCurrScreenCoord.getX(), (float) this.mCurrScreenCoord.getY(), this.mCurrSphereCoord);
            //rotationAxis is the crossproduct between the two resultant vectors (normalized)
            Vector3 rotationAxis = this.mPrevSphereCoord.clone();
            rotationAxis.cross(this.mCurrSphereCoord);
            rotationAxis.normalize();
            //rotationAngle is the acos of the vectors' dot product
            double rotationAngle = Math.acos(Math.min(1.0D, this.mPrevSphereCoord.dot(this.mCurrSphereCoord)));
            //creates a quaternion using rotantionAngle and rotationAxis (normalized)
//            this.mCurrentOrientation.fromAngleAxis(rotationAxis.inverse(), MathUtil.radiansToDegrees(-rotationAngle));
//            rotationAxis.setAll(rotationAxis.x,rotationAxis.y,rotationAxis.z);
            this.mCurrentOrientation.fromAngleAxis(rotationAxis.inverse(), MathUtil.radiansToDegrees(-rotationAngle));
            this.mCurrentOrientation.normalize();
            //accumulates start and current orientation in mEmpty object
            Quaternion q = new Quaternion(this.mStartOrientation);
            q.multiply(this.mCurrentOrientation);
            double orientacionX = q.angleBetween(new Quaternion(1f,0f,0f,0f));
            double orientacionY = q.angleBetween(new Quaternion(0f,1f,0f,0f));
            double orientacionZ = q.angleBetween(new Quaternion(0f, 0f, 1f, 0f));
            Log.e("ROTACION","angulo con x "+orientacionX);
            Log.e("ROTACION","angulo con y "+orientacionY);
            Log.e("ROTACION","angulo con z "+orientacionZ);
            q=corregirRotacion(orientacionX,orientacionY,orientacionZ,q);
            this.mEmpty.setOrientation(q);
            this.orientacionAnterior=q;
        }

    }

    /**Corrects the orientation so that the sphere doesn't flip upside down*/
    private Quaternion corregirRotacion(double orientacionX, double orientacionY, double orientacionZ, Quaternion q){
        if((orientacionX > 0.9 && Math.abs(orientacionY-1)<0.2 && orientacionZ>0.75) ||
                (orientacionX > 1 && Math.abs(orientacionY-1)<0.3 && orientacionZ>1.5) ){
//            this.mEmpty.getOrientation(q);  //take the previous orientation
            if(orientacionAnterior!=null){
                q=orientacionAnterior;
                Log.e("ORIENTACION","cambio hecho");
            }
        }
//        return q;
        q.getXAxis();
        q.getYAxis();
        q.getZAxis();
        Log.e("ORIENTACION", "eje x " + q.getXAxis().toString());

        Plane plane = new Plane(yAxis,zAxis,zAxis.inverse());
        Log.e("ORIENTACION", "distancia al eje y "+plane.getDistanceTo(q.getYAxis()));
        return q;
    }

    /**returns the object's view matrix (used in the renderer.Onrender method)*/
    public Matrix4 getViewMatrix() {
        Matrix4 m = super.getViewMatrix();
        //if the camera has a target take its position into mScratchMatrix and store in <m>
        if(this.mTarget != null) {
            this.mScratchMatrix.identity();
            this.mScratchMatrix.translate(this.mTarget.getPosition());
            m.multiply(this.mScratchMatrix);
        }

        //take also orientation from empty object (used in applyRotation) into m
        this.mScratchMatrix.identity();
        this.mScratchMatrix.rotate(this.mEmpty.getOrientation());
        m.multiply(this.mScratchMatrix);
        //if the camera has target take the position inverse and translate to m
        if(this.mTarget != null) {
            this.mScratchVector.setAll(this.mTarget.getPosition());
            this.mScratchVector.inverse();
            this.mScratchMatrix.identity();
            this.mScratchMatrix.translate(this.mScratchVector);
            m.multiply(this.mScratchMatrix);
        }

        return m;
    }


    /** sets the camera's field of view (focal distance)*/
    public void setFieldOfView(double fieldOfView) {
        //lock the frustrum and change the field of view
        Object var3 = this.mFrustumLock;
        synchronized(this.mFrustumLock) {
            this.mStartFOV = fieldOfView;
            super.setFieldOfView(fieldOfView);
        }
    }

    /** adds the basic listeners to the camera*/
    private void addListeners() {
        //runs this on the ui thread
        ((Activity)this.mContext).runOnUiThread(new Runnable() {
            public void run() {
                //sets a gesture detector (touch)
                mArcballCamera.this.mDetector = new GestureDetector(mArcballCamera.this.mContext, mArcballCamera.this.new GestureListener());
                //sets a scale detector (zoom)
                mArcballCamera.this.mScaleDetector = new ScaleGestureDetector(mArcballCamera.this.mContext, mArcballCamera.this.new ScaleListener());
                //sets a touch listener
                mArcballCamera.this.mGestureListener = new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        //sees if it is a scale event
                        mArcballCamera.this.mScaleDetector.onTouchEvent(event);
                        if(!mArcballCamera.this.mIsScaling) {
                            //if not, delivers the event to the movement detector to start rotation
                            mArcballCamera.this.mDetector.onTouchEvent(event);
                            if(event.getAction() == 1 && mArcballCamera.this.mIsRotating) {
                                //ends the rotation if the event ended
                                mArcballCamera.this.endRotation();
                                mArcballCamera.this.mIsRotating = false;
                            }
                        }

                        return true;
                    }
                };
                //sets the touch listener
                mArcballCamera.this.mView.setOnTouchListener(mArcballCamera.this.mGestureListener);
            }
        });
    }

    /**sets the camera target*/
    public void setTarget(Object3D target) {
        this.mTarget = target;
        this.setLookAt(this.mTarget.getPosition());
    }

    /*returns the camera target*/
    public Object3D getTarget() {
        return this.mTarget;
    }

    /*scale listener*/
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private ScaleListener() {
        }

        public boolean onScale(ScaleGestureDetector detector) {
//            double fov = Math.max(30.0D, Math.min(100.0D, mArcballCamera.this.mStartFOV * (1.0D / (double)detector.getScaleFactor())));
            //scale adjusted so that the edges of the sphere dont appear in the screen
            double fov = Math.max(30.0D, Math.min(54.0D, mArcballCamera.this.mStartFOV * (1.0D / (double)detector.getScaleFactor())));
            Log.e("SCALE", "detector scale factor "+detector.getScaleFactor());
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

    /*gesture listener*/
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private GestureListener() {
        }

        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
            if(!mArcballCamera.this.mIsRotating) {
                mArcballCamera.this.originalX=event2.getX();
                mArcballCamera.this.originalY=event2.getY();
//                mArcballCamera.this.startRotation(event2.getX(), event2.getY());
                mArcballCamera.this.startRotation(mArcballCamera.this.mLastWidth/2, mArcballCamera.this.mLastHeight/2); //0,0 es la esquina superior izquierda. Buscar centro camara en algun lugar
                return false;
            } else {
                float x =  (mArcballCamera.this.mLastWidth/2) - (event2.getX() - mArcballCamera.this.originalX);
                float y = (mArcballCamera.this.mLastHeight/2) - (event2.getY() - mArcballCamera.this.originalY);
                mArcballCamera.this.mIsRotating = true;
//                mArcballCamera.this.updateRotation(event2.getX(), event2.getY());
                mArcballCamera.this.updateRotation(x, y);
                return false;
            }
        }
    }
}
