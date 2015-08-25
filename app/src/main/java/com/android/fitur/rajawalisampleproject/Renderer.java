package com.android.fitur.rajawalisampleproject;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ToggleButton;

import org.rajawali3d.Object3D;
import org.rajawali3d.cameras.ArcballCamera;
import org.rajawali3d.cameras.Camera;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;

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
//TODO: modo cardboard.
//TODO: linkear con app principal. funcionalidad del backButton.
//TODO: add animaciones o efectos en los elementos con los que el usuario puede interactuar.
//TODO: hacer que el control del video se desvanezca con inactividad / desaparezca con click.
//TODO: arreglar problema al bloquar y desbloquear la pantalla sin parar de reproducir. Pasa en tablet, en movil no.
//TODO: que no rote la esfera si deslizas sobre la barra de controles.
//TODO: poner en los textview del control del video el tiempo total y el actual.

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
    public int videoLength;         //video length in ms
    private final int touchMode=0;
    private final int gyroMode=1;
    private final int cardboardMode=2;
    private int mode;
    private CamaraActualizada arcballCamera;

    /**Renderer constructor, initializes its main values*/
    public Renderer(Context context){
        super(context);
        this.context = context;
        setFrameRate(60);
        mode=touchMode;
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
                mp.start();
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
        //invert the sphere (to display the video on the inside
//        earthSphere.setScale();
        earthSphere.setScaleX(1.15);
        earthSphere.setScaleY(1.15);
        earthSphere.setScaleZ(-1.15);

//        getCurrentCamera().setPosition(0, 0, 1);
        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());
        Log.e("ESFERA", "camara en cero? " + getCurrentCamera().getPosition());
        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());

        //create the arcball camera and target the sphere
//        mArcballCamera arcballCamera = new mArcballCamera(context,MainActivity.principal,earthSphere);
        arcballCamera = new CamaraActualizada(context,MainActivity.principal,earthSphere);
//        Cam2d arcballCamera = new Cam2d(context, MainActivity.principal,earthSphere);
//        NuevaCamara arcballCamera = new NuevaCamara(context,MainActivity.principal,earthSphere);
        Log.e("CAMARA", "camara creada");
//        ArcballCamera arcballCamera = new ArcballCamera(context,MainActivity.principal,earthSphere);
//        arcballCamera.setPosition(0, 0, 5);
        Log.e("CAMARA", "camara movida");
        //switch cameras
        getCurrentScene().replaceAndSwitchCamera(getCurrentCamera(), arcballCamera);
        arcballCamera.setPosition(0, 0, 0.5);
        Log.e("CAMARA", "switch camara");
        //start the player
//        mMediaPlayer.start();
        Log.e("CAMARA", "player start");

//        Matrix4 projection = new Matrix4();
//        arcballCamera.getProjectionMatrix();

        arcballCamera.setProjectionMatrix(getViewportWidth(),getViewportHeight());
        for(int i=0; i<16; i++){
            Log.e("PROY", "elemento "+i+" "+arcballCamera.getProjectionMatrix().getDoubleValues()[i]);
        }

        videoLength=mMediaPlayer.getDuration();

//        arcballCamera.setProjectionMatrix(arcballCamera.getFieldOfView(),getViewportWidth(),getViewportHeight());

    }

    /*update the video texture on rendering*/
    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        super.onRender(elapsedTime, deltaTime);
//        Log.e("CAMARA", "super onRender");
        if (video != null) {
            video.update();
//            Log.e("CAMARA", "video updated");
        }
//        getCurrentCamera().setCameraRoll(0);
//        earthSphere.setRotZ(0);
//        Log.e("ROT","correccion");
//        android.opengl.GLES20.glFlush();
    }

    public MediaPlayer getMediaPlayer(){
        return this.mMediaPlayer;
    }

    public void toTouchMode(){
        if(mode!=touchMode){
            mode=touchMode;
            arcballCamera.switchMode(mode);
        }
    }

    public void toGyroMode(){
        if(mode!=gyroMode){
            mode=gyroMode;
            arcballCamera.switchMode(mode);
            Log.e("GYRO","de renderer a camara");
        }
    }

    public void toCardboardMode(){
        if(mode!=cardboardMode){
            mode=cardboardMode;
//            getCurrentCamera().switchMode(mode);
        }
    }
}
