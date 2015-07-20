package com.android.fitur.rajawalisampleproject;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.EGLConfig;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;

import org.rajawali3d.lights.ALight;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Plane;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;



/**
 * Created by Fitur on 17/07/2015.
 */
public class Renderer extends RajawaliRenderer{
    private Context context;
    private DirectionalLight directionalLight;
    private Sphere earthSphere;
    private ALight mLight;
    private MediaPlayer mMediaPlayer;
    private SurfaceTexture mTexture;

    /*Default renderer constructor*/
    public Renderer(Context context){
        super(context);
        this.context = context;
        setFrameRate(60);
    }

    public void onTouchEvent(MotionEvent event){
//        super.onTouchEvent(event);
//        earthSphere.rotate(Vector3.Axis.Y, 1.0);
    }

    public void onOffsetsChanged(float x, float y, float z, float w, int i, int j){
    }

    public void initScene(){
      //FIRST EXAMPLE, PNG TEXTURING OVER A SPHERE
        //set the light for our scene
        directionalLight = new DirectionalLight(1f, .2f, -1.0f);
        directionalLight.setColor(1.0f, 1.0f, 1.0f);
        directionalLight.setPower(2);
//        getCurrentScene().addLight(directionalLight);
        //set the material of the sphere
        Material material = new Material();
//        material.enableLighting(true);
//        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.setColor(0);
        //set the sphere texture
//        Texture earthTexture = new Texture("Earth", R.drawable.pyrex);
        Texture earthTexture = new Texture("Earth", R.drawable.pyrex);
        try{
            material.addTexture(earthTexture);

        } catch (ATexture.TextureException error){
            Log.d("DEBUG", "TEXTURE ERROR");
        }
        //instantiate the sphere, set the camera back
        earthSphere = new Sphere(1, 24, 24);
        //try to turn things inside the sphere
//        earthSphere.setScaleX(-1);
//        earthSphere.setScaleY(1);
//        earthSphere.setScaleZ(1);
        earthSphere.setMaterial(material);
        earthSphere.setPosition(0, 0, 0);
        Log.e("ESFERA", "is double sided " + earthSphere.isDoubleSided());
//        earthSphere.setDoubleSided(true);
        getCurrentScene().addChild(earthSphere);
//        getCurrentCamera().setFieldOfView(60);
//        earthSphere.enableLookAt();
//        earthSphere.resetToLookAt();
        Log.e("ESFERA", "posicion " + earthSphere.getPosition());
        Log.e("ESFERA", "camara " + getCurrentCamera().getPosition());
        earthSphere.setScale(2.0);
        earthSphere.setScaleX(1);
        earthSphere.setScaleY(1);
        earthSphere.setScaleZ(-1);
        getCurrentCamera().setPosition(0, 0, 1);
        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());
        Log.e("ESFERA","look at de la esfera "+earthSphere.getLookAt());
//        getCurrentCamera().setLookAt(0,4,0);
        Log.e("ESFERA", "camara en cero? " + getCurrentCamera().getPosition());
        Log.e("ESFERA", "camara mirando a " + getCurrentCamera().getLookAt());

//        getCurrentCamera().setPosition(0,0,0);
//        getCurrentCamera().setLookAt(earthSphere.getLookAt());
//        getCurrentCamera().setZ(4.2f);
//        getCurrentCamera().setPosition(0,0,0);
    }

    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        super.onRender(elapsedTime, deltaTime);
//        earthSphere.rotate(Vector3.Axis.Y, 1.0);
    }
}
