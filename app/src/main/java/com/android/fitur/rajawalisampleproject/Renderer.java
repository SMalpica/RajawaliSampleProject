package com.android.fitur.rajawalisampleproject;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.cameras.ArcballCamera;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;

import java.io.IOException;

//TODO: cambiar esfera predeterminada por esfera bien hecha
//NotTODO: ArcBallCamera para eventos de tacto y etcétera (HECHO)
//TODO: regular touch de arcballCamera y extender para eventos de giroscopio
//NotTODO: crear una instancia del android video player normal y ponerla como imagen
//TODO: Anyadir controles del reproductor
//TODO: comprobar funcionamiento video

/**
 * Created by Fitur on 17/07/2015.
 */
public class Renderer extends RajawaliRenderer {
    private Context context;
//    private DirectionalLight directionalLight;
    private Sphere earthSphere;
//    private ALight mLight;
    private MediaPlayer mMediaPlayer;
//    private SurfaceTexture mTexture;
    StreamingTexture video;

    public Renderer(Context context){
        super(context);
        this.context = context;
        setFrameRate(60);
    }

    public void onTouchEvent(MotionEvent event){
    }

    public void onOffsetsChanged(float x, float y, float z, float w, int i, int j){
    }

    public void initScene(){
        earthSphere = new Sphere(1, 24, 24);
        mMediaPlayer = new MediaPlayer();
        try{
            mMediaPlayer.setDataSource(context, Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.pyrex));
        }catch(IOException ex){
            Log.e("ERROR","couldn attach data source to the media player");
        }
        mMediaPlayer.setLooping(true);
        video = new StreamingTexture("pyrex",mMediaPlayer);
        mMediaPlayer.prepareAsync();
        Material material = new Material ();
        material.setColorInfluence(0f);
        try{
            material.addTexture(video);
        }catch(ATexture.TextureException ex){
            Log.e("ERROR","texture error when adding video to material");
        }
        earthSphere.setMaterial(material);
        earthSphere.setPosition(0, 0, 0);
        getCurrentScene().addChild(earthSphere);
        Log.e("ESFERA", "posicion " + earthSphere.getPosition());
        Log.e("ESFERA", "camara " + getCurrentCamera().getPosition());
        earthSphere.setScale(2.0);
        earthSphere.setScaleX(1);
        earthSphere.setScaleY(1);
        earthSphere.setScaleZ(-1);
        getCurrentCamera().setPosition(0, 0, 1);
        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());
        Log.e("ESFERA", "camara en cero? " + getCurrentCamera().getPosition());
        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());


//        ArcballCamera arcballCamera = new ArcballCamera(context,MainActivity.principal,earthSphere);
        mArcballCamera arcballCamera = new mArcballCamera(context,MainActivity.principal,earthSphere);
        arcballCamera.setPosition(0, 0, 5);

        getCurrentScene().replaceAndSwitchCamera(getCurrentCamera(), arcballCamera);
        arcballCamera.setPosition(0, 0, 1);


        mMediaPlayer.start();
    }

    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        super.onRender(elapsedTime, deltaTime);
        if (video != null) {
            video.update();
        }
    }
}
