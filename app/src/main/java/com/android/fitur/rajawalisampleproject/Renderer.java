package com.android.fitur.rajawalisampleproject;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.cameras.Camera;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;
import org.rajawali3d.renderer.RenderTarget;
import org.rajawali3d.scene.RajawaliScene;

import java.io.IOException;

//notTODO: cambiar esfera predeterminada por esfera bien hecha
//NotTODO: ArcBallCamera para eventos de tacto y etcétera (HECHO)
//notTODO: regular touch de arcballCamera(VIP) y (BOTON AÑADIDO)extender para eventos de giroscopio (add button)
//NotTODO: crear una instancia del android video player normal y ponerla como imagen
//notTODO: Anyadir controles del reproductor
//notTODO: comprobar funcionamiento video. EL vídeo va bien.
//TODO: arreglar inclinacion del video
//notTODO: uses-permission android:name="android.permission.WAKE_LOCK" evitar q pantalla se bloquee
//notTODO: cambiar de proyeccion fisheye a rectilinea. Aproximacion conseguida
//notTODO: ajustar size iconos playerControl. Cardboard demasiado grande, play small.
//notTODO: cambiar apariencia del seekBar. El default es feo con ganas.
//notTODO: actualizar seekBar cada segundo. Hecho
//TODO: modo giroscopo. No se mueve como deberia. Tal vez porque no estamos en la orientacion default en movil. Arreglar.
//TODO: modo cardboard. Probado: rajawalisidebysiderenderer (no se ve bien), usando planes en vez de quads (no se ve nada). A probar: VRrenderer, todo en constructor.
//notTODO: linkear con app principal. funcionalidad del backButton.
//TODO: add animaciones o efectos en los elementos con los que el usuario puede interactuar.
//notTODO: hacer que el control del video se desvanezca con inactividad / desaparezca con click.
//notTODO: arreglar problema al bloquar y desbloquear la pantalla sin parar de reproducir. Pasa en tablet, en movil no.
//TODO: comprobar correcto funcionamiento en tablet
//TODO: que no rote la esfera si deslizas sobre la barra de controles.
//notTODO: poner en los textview del control del video el tiempo total y el actual.
//notTODO: seekbar deja de funcionar al bloquear y desbloquear el movil
//TODO: . The Main Activity should deactivate the sensors whenever it is not running so that power can be saved

/**
 * Author: Sandra Malpica Mallo
 * Date: 17/07/2015.
 * Class: Renderer.java
 * Comments: app renderer. Inits the scene with a sphere and projects a video on it with an arcball
 * camera to let the user move around.
 */
public class Renderer extends RajawaliRenderer {
    private Context context;        //renderer context
    private Sphere earthSphere;     //sphere where the video will be displayed
    private MediaPlayer mMediaPlayer;   //mediaPLayer that holds the video
    StreamingTexture video;         //video texture to project on the sphere
    public int pausedPosition;         //video length in ms
    private final int touchMode=0;  //touch mode. the sphere is rotated by touching the screen
    private final int gyroMode=1;   //gyro mode. the sphere is rotated using the devices sensors
    private final int cardboardMode=2;//cardboard mode.
    private int mode;               //actual playback mode
    private CamaraActualizada arcballCamera;    //arcballCamera that looks at the sphere
    private Sphere leftSphere;
    private Sphere rightSphere;
    /****************************************************************************************/
    /*stores the camera orientation, set by the rotation vector sensor*/
    private Quaternion cameraOrientation = new Quaternion();
    private Quaternion scratchQuaternion1 = new Quaternion();
    private Quaternion scratchQuaternion2 = new Quaternion();
    /*
	 * Camera orientation lock. Used to change the camera's
	 * orientation in a thread-safe manner.
	 **/
    private final Object cameraLock = new Object();
    private Camera cameraLeft;
    private Camera cameraRight;
    /**
     * Half the width of the viewport. The screen will be split in two.
     * One half for the left eye and one half for the right eye.
     */
    private int viewPortWidthHalf;
    /**
     * The texture that will be used to render the scene into from the
     * perspective of the left eye.
     */
    private RenderTarget mLeftRenderTarget;
    /**
     * The texture that will be used to render the scene into from the
     * perspective of the right eye.
     */
    private RenderTarget mRightRenderTarget;
    private RajawaliScene userScene;/** Used to store a reference to the user's scene.*/
    /**
     * The side by side scene is what will finally be shown onto the screen.
     * This scene contains two quads. The left quad is the scene as viewed
     * from the left eye. The right quad is the scene as viewed from the
     * right eye.
     */
    private RajawaliScene mSideBySideScene;
    /**
     * This screen quad will contain the scene as viewed from the left eye.
     */
    private ScreenQuad mLeftQuad;
//    private Plane mLeftPlane;
    /**
     * This screen quad will contain the scene as viewed from the right eye.
     */
    private ScreenQuad mRightQuad;
//    private Plane mRightPlane;
    /**
     * The material for the left quad
     */
    private Material mLeftQuadMaterial;
    /**
     * The material for the right quad
     */
    private Material mRightQuadMaterial;
    /**
     * The distance between the pupils. This is used to offset the cameras.
     */
    private double mPupilDistance = .06;
    /****************************************************************************************/

    /**Renderer constructor, initializes its main values*/
    public Renderer(Context context){
        super(context);
        this.context = context;
        setFrameRate(30);   //sets the renderer frame rate
        mode=touchMode;     //initial mode: touch mode
        //TODO: left and right camera, with different spheres?
        //look at https://github.com/google/grafika/tree/master/src/com/android/grafika
        //https://github.com/google/grafika/blob/master/src/com/android/grafika/MultiSurfaceActivity.java
        //http://stackoverflow.com/questions/22529981/android-camera-preview-on-two-views-multi-lenses-camera-preview
        //https://github.com/Sveder/CardboardPassthrough
        //http://stackoverflow.com/questions/9834882/how-to-play-multiple-video-files-simultaneously-in-one-layout-side-by-side-in-di
        //https://github.com/Rajawali/Rajawali/blob/master/rajawali/src/main/java/org/rajawali3d/renderer/RajawaliSideBySideRenderer.java#L183
        //https://github.com/Rajawali/Rajawali/issues/989
        //http://stackoverflow.com/questions/10604047/setting-up-viewport-for-opengl-es-android
        //http://iphonedevelopment.blogspot.com.es/2009/04/opengl-es-from-ground-up-part-3.html
        //http://stackoverflow.com/questions/30431985/playing-2-mediaplayers-at-same-time-together-playing-only-one-mediaplayer-in-and
        //http://developer.android.com/reference/android/view/SurfaceHolder.Callback.html#surfaceChanged(android.view.SurfaceHolder, int, int, int)
        //http://stackoverflow.com/questions/27530951/how-do-i-change-the-google-cardboard-stereo-view-to-a-mono-view?rq=1
    }

    public void onTouchEvent(MotionEvent event){
    }

    public void onOffsetsChanged(float x, float y, float z, float w, int i, int j){
    }

    /**this method is called to set up the sphere*/
    public void initScene(){
        //create a 100 segment sphere
        earthSphere = new Sphere(1, 100, 100);

        //try to set the mediaPLayer data source
        mMediaPlayer = new MediaPlayer();
        try{
            mMediaPlayer.setDataSource(context, Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.pyrex));
        }catch(IOException ex){
            Log.e("ERROR","couldn attach data source to the media player");
        }
        mMediaPlayer.setLooping(true);  //enable video looping
        video = new StreamingTexture("pyrex",mMediaPlayer); //create video texture
        mMediaPlayer.prepareAsync();    //prepare the player (asynchronous)
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start(); //start the player only when it is prepared
            }
        });
        //add textture to a new material
        Material material = new Material ();
        material.setColorInfluence(0f);
        try{
            material.addTexture(video);
        }catch(ATexture.TextureException ex){
            Log.e("ERROR","texture error when adding video to material");
        }
        //set the material to the sphere
        earthSphere.setMaterial(material);
        earthSphere.setPosition(0, 0, 0);
        //add the sphere to the rendering scene
        getCurrentScene().addChild(earthSphere);
        Log.e("ESFERA", "posicion " + earthSphere.getPosition());
        Log.e("ESFERA", "camara " + getCurrentCamera().getPosition());
        //invert the sphere (to display the video on the inside of the sphere)
        earthSphere.setScaleX(1.15);
        earthSphere.setScaleY(1.15);
        earthSphere.setScaleZ(-1.15);

        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());
        Log.e("ESFERA", "camara en cero? " + getCurrentCamera().getPosition());
        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());

        //create the arcball camera and target the sphere
        arcballCamera = new CamaraActualizada(context,MainActivity.principal,earthSphere);
        Log.e("CAMARA", "camara creada");
        Log.e("CAMARA", "camara movida");
        //switch cameras
        getCurrentScene().replaceAndSwitchCamera(getCurrentCamera(), arcballCamera);
        arcballCamera.setPosition(0, 0, 0.5);
        Log.e("CAMARA", "switch camara");

        /****************************************************************************************/
        //initialize left camera
        cameraLeft = new Camera();
//        cameraLeft.setNearPlane(.01f);
        cameraLeft.setFieldOfView(getCurrentCamera().getFieldOfView());
        cameraLeft.setNearPlane(getCurrentCamera().getNearPlane());
        cameraLeft.setFarPlane(getCurrentCamera().getFarPlane());
        //initialize right camera
        cameraRight = new Camera();
//        cameraRight.setNearPlane(.01f);
        cameraRight.setFieldOfView(getCurrentCamera().getFieldOfView());
        cameraRight.setNearPlane(getCurrentCamera().getNearPlane());
        cameraRight.setFarPlane(getCurrentCamera().getFarPlane());
        //set pupil distance
        setPupilDistance(mPupilDistance);
        //set quads materials
        mLeftQuadMaterial = new Material();
        mLeftQuadMaterial.setColorInfluence(0);
        mRightQuadMaterial = new Material();
        mRightQuadMaterial.setColorInfluence(0);
        //initialize sidebyside scene
        mSideBySideScene = new RajawaliScene(this);
        //initialize quads(left)
        mLeftQuad = new ScreenQuad();
//        mLeftPlane = new Plane(viewPortWidthHalf,mDefaultViewportHeight,100,100);
//        mLeftQuad.setScaleX(1.5);
        mLeftQuad.setX(-.25);
//        mLeftPlane.setX(-.25);
        mLeftQuad.setMaterial(mLeftQuadMaterial);
//        mLeftPlane.setMaterial(mLeftQuadMaterial);
        mSideBySideScene.addChild(mLeftQuad);
//        mSideBySideScene.addChild(mLeftPlane);
        //(right)
        mRightQuad = new ScreenQuad();
//        mRightPlane = new Plane(viewPortWidthHalf,mDefaultViewportHeight,100,100);
//        mRightQuad.setScaleX(1.5);
        mRightQuad.setX(.25);
//        mRightPlane.setX(.25);
        mRightQuad.setMaterial(mRightQuadMaterial);
//        mRightPlane.setMaterial(mRightQuadMaterial);
        mSideBySideScene.addChild(mRightQuad);
//        mSideBySideScene.addChild(mRightPlane);
        //add created scene
        addScene(mSideBySideScene);
        //set half viewport width value
        viewPortWidthHalf = (int) (mDefaultViewportWidth * .5f);
        //set both render targets
        mLeftRenderTarget = new RenderTarget("sbsLeftRT", viewPortWidthHalf, mDefaultViewportHeight);
        mLeftRenderTarget.setFullscreen(false);
        mRightRenderTarget = new RenderTarget("sbsRightRT", viewPortWidthHalf, mDefaultViewportHeight);
        mRightRenderTarget.setFullscreen(false);
        //set each camera's projection matrices
        cameraLeft.setProjectionMatrix(viewPortWidthHalf, mDefaultViewportHeight);
        cameraRight.setProjectionMatrix(viewPortWidthHalf, mDefaultViewportHeight);
        //add quads to this renderer target
        addRenderTarget(mLeftRenderTarget);
        addRenderTarget(mRightRenderTarget);
        //add textures
        try {
            mLeftQuadMaterial.addTexture(mLeftRenderTarget.getTexture());
            mRightQuadMaterial.addTexture(mRightRenderTarget.getTexture());
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        /****************************************************************************************/
    }

    /*update the video texture on rendering*/
    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        super.onRender(elapsedTime, deltaTime);
        if (video != null) {
            video.update();
        }
        //if the screen is off, pause the mediaPlayer and store the current position for later restoring
        PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (!mPowerManager.isScreenOn()){
            if (mMediaPlayer!= null && mMediaPlayer.isPlaying())
            {    mMediaPlayer.pause();
                pausedPosition=mMediaPlayer.getCurrentPosition();
            }
        }
        /****************************************************************************************/
        if(mode == cardboardMode){
            userScene = getCurrentScene();

            setRenderTarget(mLeftRenderTarget);
            getCurrentScene().switchCamera(cameraLeft);
            GLES20.glViewport(0, 0, viewPortWidthHalf, mDefaultViewportHeight); //only half the screen size
            cameraLeft.setProjectionMatrix(viewPortWidthHalf, mDefaultViewportHeight);
            cameraLeft.setOrientation(cameraOrientation);

            render(elapsedTime, deltaTime);

            setRenderTarget(mRightRenderTarget);
            getCurrentScene().switchCamera(cameraRight);
            cameraRight.setProjectionMatrix(viewPortWidthHalf, mDefaultViewportHeight);
            cameraRight.setOrientation(cameraOrientation);

            render(elapsedTime, deltaTime);

            switchSceneDirect(mSideBySideScene);
            GLES20.glViewport(0, 0, mDefaultViewportWidth, mDefaultViewportHeight);

            setRenderTarget(null);
            render(elapsedTime, deltaTime);
            switchSceneDirect(userScene);
        }
        /****************************************************************************************/
    }

    //returns this renderer media player
    public MediaPlayer getMediaPlayer(){
        return this.mMediaPlayer;
    }

    //changes renderer to touch mode
    public void toTouchMode(){
        if(mode!=touchMode){
            mode=touchMode;
            arcballCamera.switchMode(mode);
        }
    }

    //changes renderer to gyro mode
    public void toGyroMode(){
        if(mode!=gyroMode){
            mode=gyroMode;
            arcballCamera.switchMode(mode);
            Log.e("GYRO", "de renderer a camara");
        }
    }

    //changes renderer to cardboard mode
    public void toCardboardMode(){
        if(mode!=cardboardMode){
            mode=cardboardMode;
            arcballCamera.switchMode(mode);
        }
    }

    //called when the renderer is paused. it pauses the renderer and the mediaPlayer
    //storing its position
    @Override
    public void onPause(){
        super.onPause();
        if (mMediaPlayer!= null && mMediaPlayer.isPlaying())
        {    mMediaPlayer.pause();
            pausedPosition=mMediaPlayer.getCurrentPosition();
        }
    }

    //called when the renderer is resumed from a pause.
    //resumes mediaPlayer state
    @Override
    public void onResume(){
        super.onResume();
        if(mMediaPlayer!=null){
            mMediaPlayer.seekTo(pausedPosition);
        }
    }

    public void setPupilDistance(double pupilDistance)
    {
        mPupilDistance = pupilDistance;
        if (cameraLeft != null)
            cameraLeft.setX(pupilDistance * -.5);
        if (cameraLeft != null)
            cameraRight.setX(pupilDistance * .5);
    }

    public void setCameraOrientation(Quaternion cameraOrientation) {
        synchronized (cameraLock) {
            cameraOrientation.setAll(cameraOrientation);
        }
    }

    public void setSensorOrientation(float[] quaternion)
    {
        synchronized (cameraLock) {
            cameraOrientation.x = quaternion[1];
            cameraOrientation.y = quaternion[2];
            cameraOrientation.z = quaternion[3];
            cameraOrientation.w = quaternion[0];

//            scratchQuaternion1.fromAngleAxis(Vector3.Axis.X, -90);
            scratchQuaternion1=new Quaternion();
            scratchQuaternion1.multiply(cameraOrientation);

            scratchQuaternion2.fromAngleAxis(Vector3.Axis.Z, +180);
            scratchQuaternion1.multiply(scratchQuaternion2);

            cameraOrientation.setAll(scratchQuaternion1);

        }
    }
}
