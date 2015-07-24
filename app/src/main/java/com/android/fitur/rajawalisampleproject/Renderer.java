package com.android.fitur.rajawalisampleproject;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;

import java.io.IOException;

//notTODO: cambiar esfera predeterminada por esfera bien hecha
//NotTODO: ArcBallCamera para eventos de tacto y etcétera (HECHO)
//TODO: regular touch de arcballCamera(VIP) y extender para eventos de giroscopio (add button)
//NotTODO: crear una instancia del android video player normal y ponerla como imagen
//TODO: Anyadir controles del reproductor
//TODO: comprobar funcionamiento video

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

    /**Renderer constructor, initializes its main values*/
    public Renderer(Context context){
        super(context);
        this.context = context;
        setFrameRate(60);
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
        earthSphere.setScale(2.0);
        earthSphere.setScaleX(1);
        earthSphere.setScaleY(1);
        earthSphere.setScaleZ(-1);

        getCurrentCamera().setPosition(0, 0, 1);
        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());
        Log.e("ESFERA", "camara en cero? " + getCurrentCamera().getPosition());
        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());

        //create the arcball camera and target the sphere
        mArcballCamera arcballCamera = new mArcballCamera(context,MainActivity.principal,earthSphere);
        arcballCamera.setPosition(0, 0, 5);
        //switch cameras
        getCurrentScene().replaceAndSwitchCamera(getCurrentCamera(), arcballCamera);
        arcballCamera.setPosition(0, 0, 1);
        //start the player
        mMediaPlayer.start();
    }

    /*update the video texture on rendering*/
    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        super.onRender(elapsedTime, deltaTime);
        if (video != null) {
            video.update();
        }
    }
}
