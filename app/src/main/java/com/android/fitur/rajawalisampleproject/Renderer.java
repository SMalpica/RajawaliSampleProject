package com.android.fitur.rajawalisampleproject;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
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
//notTODO: hacer que el control del video se desvanezca con inactividad / desaparezca con click.
//notTODO: arreglar problema al bloquar y desbloquear la pantalla sin parar de reproducir. Pasa en tablet, en movil no.
//TODO: comprobar correcto funcionamiento en tablet
//TODO: que no rote la esfera si deslizas sobre la barra de controles.
//notTODO: poner en los textview del control del video el tiempo total y el actual.
//notTODO: seekbar deja de funcionar al bloquear y desbloquear el movil

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

    /**Renderer constructor, initializes its main values*/
    public Renderer(Context context){
        super(context);
        this.context = context;
        setFrameRate(30);   //sets the renderer frame rate
        mode=touchMode;     //initial mode: touch mode
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
}
