package com.android.fitur.rajawalisampleproject;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliSideBySideRenderer;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Author: Sandra Malpica Mallo
 * Date: 17/07/2015.
 * Class: MainActivity.java
 * Comments: main video app class, holds video activity. Creates a rajawali surface and sets things up.
 */
public class MainActivity extends RajawaliVRActivity implements SeekBar.OnSeekBarChangeListener,View.OnClickListener{

//    Renderer renderer;              //openGL renderer
    RajawaliSideBySideRenderer sideRenderer;
    RajawaliSurfaceView surface;    //openGL surface
    public static View principal;   //surface, for external access
    public static View control;     //view, control video view, for external access
    public LinearLayout view;       //view, control video view
    private TextView tiempoActual;  //textview to show the actual reproduced video time
    private TextView tiempoTotal;   //textview to show the total video time
    private Thread controller;      //updates progress in seekbar and textviews
    public static CountDownTimer timer;//timer to make the control video view invisible when inactive
    public Semaphore lock;          //stops controller thread when app is paused
    private int modo=0;             //video playback mode: touch(0), gyro(1), cardboard(2)
    private int tTotal,tActual;     //current and total video time
    private boolean tieneGiro = false;//shows if the device has a gyroscope sensor
    private CRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        lock=new Semaphore(1);  //initializes the semaphore
        //full-screen
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //horizontal orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //set the renderer
        mRenderer = new CRenderer(this);
        setRenderer(mRenderer);
        //keep the screen on
        getSurfaceView().setKeepScreenOn(true);
        principal = getSurfaceView();
        getSurfaceView().setVRModeEnabled(false);

        //inflates the video control view and adds it to current viewGroup
        LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
        ViewGroup viewGroup = (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content);
        view = (LinearLayout)layoutInflater.inflate(R.layout.player_control, null);
        view.setVerticalGravity(Gravity.BOTTOM);
        tiempoActual = (TextView)view.findViewById(R.id.tiempoTranscurrido);
        tiempoTotal = (TextView)view.findViewById(R.id.tiempoTotal);
        viewGroup.addView(view);
        //set the controls invisible when 3s pass and the user doesnt touch them.
        timer=new CountDownTimer(7000,7000){
            public void onFinish(){
                MainActivity.this.view.setVisibility(View.INVISIBLE);
            }
            public void onTick(long l){}
        };
        timer.start();

        //set listeners to the buttons and seekbar progress control
        final ImageButton playButton = (ImageButton) view.findViewById(R.id.playbutton);
        ImageButton backButton = (ImageButton) view.findViewById(R.id.backbutton);
        final ImageButton modeButton = (ImageButton) view.findViewById(R.id.modebutton);
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);

        //pause and play the video when playButton is pressed
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if(renderer.getMediaPlayer().isPlaying()){
                    renderer.getMediaPlayer().pause();
                    playButton.setImageLevel(1);//change button image
                }else{
                    renderer.getMediaPlayer().start();
                    playButton.setImageLevel(0);
                }*/
                if(mRenderer.getMediaPlayer().isPlaying()){
                    mRenderer.getMediaPlayer().pause();
                    playButton.setImageLevel(1);//change button image
                }else{
                    mRenderer.getMediaPlayer().start();
                    playButton.setImageLevel(0);
                }
            }
        });

        //change video playback mode
        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if the device has accelerometer and gyroscope
                //if it doesnt have the needed sensors, only touch mode will be allowed
                if(tieneGiro){
                    MainActivity.this.modo = (MainActivity.this.modo+1)%3;
                }
                switch (MainActivity.this.modo){
                    case 0:         //TOUCH MODE
                        modeButton.setImageLevel(0);    //change button image
//                        renderer.toTouchMode();         //change renderer mode
                        mRenderer.toTouchMode();
                        getSurfaceView().setVRModeEnabled(false);
                        break;
                    case 1:         //GYROSCOPE MODE
                        modeButton.setImageLevel(1);
//                        renderer.toGyroMode();
                        mRenderer.toGyroMode();
                        getSurfaceView().setVRModeEnabled(false);
                        Log.e("GYRO","de activity a renderer");
                        break;
                    case 2:         //CARDBOARD MODE
                        modeButton.setImageLevel(2);
//                        renderer.toCardboardMode();
                        mRenderer.toCardboardMode();
                        getSurfaceView().setVRModeEnabled(true);
                        break;
                }
            }
        });

        //exit the video player when backButton is pressed
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.runFinalization();
                System.exit(0); //cerrar el sistema.
            }
        });

        seekBar.setOnSeekBarChangeListener(this);
        //launch a thread to update seekBar progress (---each second----)
        controller= new Thread(new Runnable() {
            private int posicion;
            boolean primera = true;
            @Override
            public void run() {
                //run while the mediaPlayer exists
//                while(primera || renderer.getMediaPlayer()!=null){
                    while(primera || mRenderer.getMediaPlayer()!=null){
                    //acquire semaphore lock//used in onPause method
                    try{
                        lock.acquire();
                    }catch(InterruptedException ex){}
                    //wait until the mediaPlayer(prepared in the renderer) is not null
//                    while (renderer.getMediaPlayer()==null){}
                        while (mRenderer.getMediaPlayer()==null){}
                    //wait until the mediaPlayer is playing
//                    while (!renderer.getMediaPlayer().isPlaying()){}
                        while (!mRenderer.getMediaPlayer().isPlaying()){}
                    //set the total video time (only one executed once)
                    if(primera){
//                        tTotal=renderer.getMediaPlayer().getDuration()/1000;
                        tTotal=mRenderer.getMediaPlayer().getDuration()/1000;
                    }
                    //wait 1 second
                    try{
                        Thread.sleep(1000);
                        //get current mediaPlayer position
//                        posicion = renderer.getMediaPlayer().getCurrentPosition();
                        posicion = mRenderer.getMediaPlayer().getCurrentPosition();
                        tActual=posicion/1000;
                    }catch(InterruptedException ex){}
                    //sets the seekbar max to update progress with normal position
//                    seekBar.setMax(renderer.getMediaPlayer().getDuration());
                        seekBar.setMax(mRenderer.getMediaPlayer().getDuration());
                    //sends information to the UI thread
                    //UI elements can only be modified there
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress(posicion);  //sets seekbar progress
                            //sets textviews values
                            String am = String.format("%02d", tActual / 60);
                            String as = String.format("%02d", tActual % 60);
                            tiempoActual.setText(am + ":" + as);
                            if (primera) {
                                am = String.format("%02d", tTotal / 60);
                                as = String.format("%02d", tTotal % 60);
                                tiempoTotal.setText(am + ":" + as);
                                primera = false;
                            }
                        }
                    });
                    lock.release(); //releases the semaphores lock
                }

            }
        });
        controller.start(); //starts the controller thread

        control=view;
        //checks whether the device has the needed sensors
        PackageManager pm = getPackageManager();
        tieneGiro = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        tieneGiro = tieneGiro & pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) & pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
        Log.e("GYRO", "tiene acelerometro "+pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER));
        Log.e("GYRO","tiene sensor rotacion "+pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS) );
        Log.e("GYRO", "tiene giroscopo "+tieneGiro);
        Log.e("PRUEBA","estoy al final del onCreate del activity");
    }

    /*method called when the activity is created*/
    /*@Override
    public void onCreate(Bundle savedInstanceState) {
        *//******************************************************************************//*
        *//*                            Initializations                                 *//*
        *//******************************************************************************//*
        super.onCreate(savedInstanceState);
        lock=new Semaphore(1);  //initializes the semaphore
        //full-screen
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //horizontal orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //creates the surface and sets the frameRate
        surface = new RajawaliSurfaceView(this);
        surface.setFrameRate(30.0); //sets the surface's framerate
        surface.setKeepScreenOn(true);  //prevents the screen from locking itself automatically
        principal = surface;
        // Add mSurface to your root view
        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        int mitadHorizontal = dm.widthPixels/2;
        int bottom = (int)dm.heightPixels;
        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(mitadHorizontal*2,bottom);
        surface.setMinimumHeight(bottom);
//        lp.bottomMargin=bottom;
        surface.setMinimumHeight(bottom);
//        surface.setMeasuredDimension(mitadHorizontal,bottom);
//        addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));
//        getWindow().setLayout((int)dm.xdpi,bottom*2);
        addContentView(surface, lp);
//        surface.layout(0,0,mitadHorizontal,bottom);
        renderer = new Renderer(this);  //creates the renderer
        *//*sideRenderer=new RajawaliSideBySideRenderer(this) {
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
            @Override
            public void onOffsetsChanged(float v, float v1, float v2, float v3, int i, int i1) {

            }

            @Override
            public void onTouchEvent(MotionEvent motionEvent) {

            }

            @Override
            public void initScene(){
                //create a 100 segment sphere
                earthSphere = new Sphere(1, 100, 100);
                //try to set the mediaPLayer data source
                mMediaPlayer = new MediaPlayer();
                try{
                    boolean esnull = (mMediaPlayer==null);
                    Log.e("SIDE_DEBUG","media player is null: "+esnull);
                    esnull = context==null;
                    Log.e("SIDE_DEBUG","context is null: "+esnull);
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
                super.initScene();
            }
        };*//*
        surface.setSurfaceRenderer(renderer);   //sets the surface renderer
//        surface.setSurfaceRenderer(sideRenderer);
//        RajawaliSurfaceView mirror = new RajawaliSurfaceView(this);
//        mirror.setSurfaceRenderer(renderer);
//        mirror.layout(mitadHorizontal,0,(int)dm.widthPixels,bottom);
//        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(mitadHorizontal,bottom);
//        RelativeLayout relativeL = new RelativeLayout(this);
//        relativeL.setBackgroundColor(Color.CYAN);
//        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(mitadHorizontal,bottom);
//        lp2.leftMargin=mitadHorizontal;
//        Log.e("LAYOUT","x "+dm.xdpi+" y "+dm.ydpi);
//        relativeL.setHorizontalGravity(Gravity.RIGHT);
//        RelativeLayout relativeR = new RelativeLayout(this);
//        relativeL.layout(mitadHorizontal,0,mitadHorizontal*2,bottom);
//        addContentView(relativeL,lp2);
//        LinearLayout padre = new LinearLayout(this);
//        addContentView(padre, new LinearLayout.LayoutParams((int) dm.widthPixels, (int) dm.heightPixels));
//        padre.addView(surface, lp);
//        padre.addView(relativeL,lp2);
//        padre.addView(mirror,lp3);
        Log.e("DEBUG","añadido");

        *//******************************************************************************//*
        *//*                            media player controls                           *//*
        *//******************************************************************************//*
        //inflates the video control view and adds it to current viewGroup
        LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
        ViewGroup viewGroup = (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content);
        view = (LinearLayout)layoutInflater.inflate(R.layout.player_control, null);
        view.setVerticalGravity(Gravity.BOTTOM);
        tiempoActual = (TextView)view.findViewById(R.id.tiempoTranscurrido);
        tiempoTotal = (TextView)view.findViewById(R.id.tiempoTotal);
        viewGroup.addView(view);
        //set the controls invisible when 3s pass and the user doesnt touch them.
        timer=new CountDownTimer(7000,7000){
            public void onFinish(){
              MainActivity.this.view.setVisibility(View.INVISIBLE);
            }
            public void onTick(long l){}
        };
        timer.start();

        //set listeners to the buttons and seekbar progress control
        final ImageButton playButton = (ImageButton) view.findViewById(R.id.playbutton);
        ImageButton backButton = (ImageButton) view.findViewById(R.id.backbutton);
        final ImageButton modeButton = (ImageButton) view.findViewById(R.id.modebutton);
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);

        //pause and play the video when playButton is pressed
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(renderer.getMediaPlayer().isPlaying()){
                    renderer.getMediaPlayer().pause();
                    playButton.setImageLevel(1);//change button image
                }else{
                    renderer.getMediaPlayer().start();
                    playButton.setImageLevel(0);
                }
            }
        });

        //change video playback mode
        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if the device has accelerometer and gyroscope
                //if it doesnt have the needed sensors, only touch mode will be allowed
                if(tieneGiro){
                    MainActivity.this.modo = (MainActivity.this.modo+1)%3;
                }
                switch (MainActivity.this.modo){
                    case 0:         //TOUCH MODE
                        modeButton.setImageLevel(0);    //change button image
                        renderer.toTouchMode();         //change renderer mode
                        break;
                    case 1:         //GYROSCOPE MODE
                        modeButton.setImageLevel(1);
                        renderer.toGyroMode();
                        Log.e("GYRO","de activity a renderer");
                        break;
                    case 2:         //CARDBOARD MODE
                        modeButton.setImageLevel(2);
                        renderer.toCardboardMode();
                        break;
                }
            }
        });

        //exit the video player when backButton is pressed
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.runFinalization();
                System.exit(0); //cerrar el sistema.
            }
        });

        *//*seekBar tutorial from http://sapandiwakar.in/tutorial-how-to-manually-create-android-media-player-controls/ *//*
        seekBar.setOnSeekBarChangeListener(this);
        //launch a thread to update seekBar progress (---each second----)
        controller= new Thread(new Runnable() {
            private int posicion;
            boolean primera = true;
            @Override
            public void run() {
                //run while the mediaPlayer exists
                while(primera || renderer.getMediaPlayer()!=null){
                    //acquire semaphore lock//used in onPause method
                    try{
                        lock.acquire();
                    }catch(InterruptedException ex){}
                    //wait until the mediaPlayer(prepared in the renderer) is not null
                    while (renderer.getMediaPlayer()==null){}
                    //wait until the mediaPlayer is playing
                    while (!renderer.getMediaPlayer().isPlaying()){}
                    //set the total video time (only one executed once)
                    if(primera){
                        tTotal=renderer.getMediaPlayer().getDuration()/1000;
                    }
                    //wait 1 second
                    try{
                        Thread.sleep(1000);
                        //get current mediaPlayer position
                        posicion = renderer.getMediaPlayer().getCurrentPosition();
                        tActual=posicion/1000;
                    }catch(InterruptedException ex){}
                    //sets the seekbar max to update progress with normal position
                    seekBar.setMax(renderer.getMediaPlayer().getDuration());
                    //sends information to the UI thread
                    //UI elements can only be modified there
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress(posicion);  //sets seekbar progress
                            //sets textviews values
                            String am = String.format("%02d", tActual / 60);
                            String as = String.format("%02d", tActual % 60);
                            tiempoActual.setText(am + ":" + as);
                            if (primera) {
                                am = String.format("%02d", tTotal / 60);
                                as = String.format("%02d", tTotal % 60);
                                tiempoTotal.setText(am + ":" + as);
                                primera = false;
                            }
                        }
                    });
                    lock.release(); //releases the semaphores lock
                }

            }
        });
        controller.start(); //starts the controller thread

        control=view;
        //checks whether the device has the needed sensors
        PackageManager pm = getPackageManager();
        tieneGiro = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        tieneGiro = tieneGiro & pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) & pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
        Log.e("GYRO","tiene acelerometro "+pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER));
        Log.e("GYRO","tiene sensor rotacion "+pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS) );
        Log.e("GYRO", "tiene giroscopo "+tieneGiro);
        Log.e("PRUEBA","estoy al final del onCreate del activity");
    }*/


    /******************************************************************************/
    /*                            Activity methods                                */
    /******************************************************************************/

    //called when the user clicks on the screen
    @Override
    public void onClick(View v) {
        timer.cancel();
        if (MainActivity.this.view.getVisibility() == View.INVISIBLE) {
            MainActivity.this.view.setVisibility(View.VISIBLE);
            timer.start();
        } else {
            MainActivity.this.view.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //called when the activity is paused (ie screen blocked or home button pressed)
    //stops the activity, the mediaplayer and the renderer, as well as the auxiliary thread
    @Override
    public void onPause() {
        super.onPause();
//        surface.onPause();
//        renderer.onPause();
        getSurfaceView().onPause();
        mRenderer.onPause();
        try{
            lock.acquire();
        }catch(InterruptedException ex){}
        Log.e("SCREEN","onpause called");
    }

    //called when the activity is resumed
    //resumes the activity, the mediaplayer and the renderer, as well as the auxiliary thread
    //also brings back de video controller view
    @Override
    public void onResume() {
        super.onResume();
//        surface.onResume();
        //renderer.onResume();
        getSurfaceView().onResume();
        if(view.getVisibility()!=View.VISIBLE){
            view.setVisibility(View.VISIBLE);
            timer.cancel(); //restarts the timer
            timer.start();
        }
        lock.release();
        Log.e("SCREEN", "onresume called");
    }

    /********************************************************************************/
    /*                             SeekBarListener methods                          */
    /********************************************************************************/
    //called each time the seekbar progress changes
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //if the user provoked the change
        if (fromUser) {
            //change mediaPLayer position
//            renderer.getMediaPlayer().seekTo(progress);
            mRenderer.getMediaPlayer().seekTo(progress);
            seekBar.setProgress(progress);  //change the seekbar progress
            progress=progress/1000;
            //change the textview accordingly to the movement
            String am = String.format("%02d", progress / 60);
            String as = String.format("%02d", progress % 60);
            tiempoActual.setText(am + ":" + as);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
