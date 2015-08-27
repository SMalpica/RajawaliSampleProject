package com.android.fitur.rajawalisampleproject;

import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;

import org.rajawali3d.surface.RajawaliSurfaceView;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Author: Sandra Malpica Mallo
 * Date: 17/07/2015.
 * Class: MainActivity.java
 * Comments: main app class, holds main activity. Creates a rajawali surface and sets things up.
 */
public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener,View.OnClickListener{

    Renderer renderer;  //openGL renderer
    RajawaliSurfaceView surface;    //surface
    public static View principal;   //surface
    public static View control;     //view
    private int modo=0;
    private int tTotal,tActual;
    private boolean tieneGiro = false;
    private TextView tiempoActual;
    private TextView tiempoTotal;
    public static CountDownTimer timer;
    public LinearLayout view;       //view
    private Thread controller;
    public Semaphore lock;
//    private boolean tieneAccel = false;

    /*method called when the activity is created*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lock=new Semaphore(1);
//        setContentView(R.layout.activity_main);
        //full-screen
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //horizontal orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //creates the surface and sets the frameRate
        surface = new RajawaliSurfaceView(this);
        surface.setFrameRate(30.0);
        surface.setKeepScreenOn(true);
        principal = surface;
        // Add mSurface to your root view
        addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));
        renderer = new Renderer(this);
        surface.setSurfaceRenderer(renderer);

        /******************************************************************************/
        /*                            media player controls                           */
        /******************************************************************************/
        LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
        layoutInflater.inflate(R.layout.player_control, null);
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
        //also make the view reappear when the screen is touched
//        surface.setOnClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            System.out.println("onclick triggered");
//            timer.cancel();
//            if (MainActivity.this.view.getVisibility() == View.INVISIBLE) {
//                MainActivity.this.view.setVisibility(View.VISIBLE);
//                timer.start();
//            } else {
//                MainActivity.this.view.setVisibility(View.INVISIBLE);
//            }
//        }
//        });

        final ImageButton playButton = (ImageButton) view.findViewById(R.id.playbutton);
        ImageButton backButton = (ImageButton) view.findViewById(R.id.backbutton);
        final ImageButton modeButton = (ImageButton) view.findViewById(R.id.modebutton);
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(renderer.getMediaPlayer().isPlaying()){
                    renderer.getMediaPlayer().pause();
                    playButton.setImageLevel(1);
                }else{
                    renderer.getMediaPlayer().start();
                    playButton.setImageLevel(0);
                }
            }
        });


        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if the device has accelerometer and gyroscope
                if(tieneGiro){
                    MainActivity.this.modo = (MainActivity.this.modo+1)%3;
                }
                switch (MainActivity.this.modo){
                    case 0:         //TOUCH MODE
                        modeButton.setImageLevel(0);
                        renderer.toTouchMode();
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

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.runFinalization();
                System.exit(0); //cerrar el sistema. TODO: volver a Activity principal
            }
        });

        /*seekBar tutorial from http://sapandiwakar.in/tutorial-how-to-manually-create-android-media-player-controls/ */
        seekBar.setOnSeekBarChangeListener(this);
        //launch a thread to update seekBar progress (---each second----)

        controller= new Thread(new Runnable() {
            private int posicion;
            boolean primera = true;
            @Override
            public void run() {
                while(primera || renderer.getMediaPlayer()!=null){
                    try{
                        lock.acquire();
                    }catch(InterruptedException ex){}
                    while (renderer.getMediaPlayer()==null){}
                    while (!renderer.getMediaPlayer().isPlaying()){}
                    if(primera){
                        tTotal=renderer.getMediaPlayer().getDuration()/1000;
                    }

                    try{
                        Thread.sleep(1000);
                        posicion = renderer.getMediaPlayer().getCurrentPosition();
                    tActual=posicion/1000;
                    }catch(InterruptedException ex){}
                    seekBar.setMax(renderer.getMediaPlayer().getDuration());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress(posicion);
                            String am = String.format("%02d", tActual / 60);
                            String as = String.format("%02d", tActual % 60);
                            tiempoActual.setText(am + ":" + as);
                            if (primera) {
                                am = String.format("%02d", tTotal / 60);
                                as = String.format("%02d", tTotal % 60);
                                System.out.println("am es " + am + " y su longitud " + am.length());
                                tiempoTotal.setText(am + ":" + as);
                                System.out.println("prueba " + tTotal);
                                primera = false;
                            }
                        }
                    });
                    lock.release();
                }

            }
        });
        controller.start();
        control=view;
        PackageManager pm = getPackageManager();
        tieneGiro = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        Log.e("GYRO","tiene acelerometro "+pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER));
        Log.e("GYRO","tiene sensor rotacion "+pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS) );
        Log.e("GYRO", "tiene giroscopo "+tieneGiro);
        Log.e("PRUEBA","estoy al final del onCreate del activity");
    }

    @Override
    public void onClick(View v) {
        System.out.println("onclick triggered");
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

    @Override
    protected void onPause() {
        super.onPause();
        surface.onPause();
        renderer.onPause();
//        if(renderer!=null && renderer.getMediaPlayer()!=null){
//            renderer.getMediaPlayer().pause();
//        }
        try{
            lock.acquire();
        }catch(InterruptedException ex){}

    }

    @Override
    protected void onResume() {
        super.onResume();
        surface.onResume();
        renderer.onResume();
        if(renderer!=null && renderer.getMediaPlayer()!=null){
            renderer.getMediaPlayer().seekTo(renderer.pausedPosition);
        }
        if(view.getVisibility()!=View.VISIBLE){
            view.setVisibility(View.VISIBLE);
        }
        lock.release();
    }

    /********************************************************************************/
    /*                             SeekBarListener methods                          */
    /********************************************************************************/
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        Log.e("SEEKBAR", "progress " + progress);
        if (fromUser) {
//            int posicion = progress * (renderer.videoLength/100);
            renderer.getMediaPlayer().seekTo(progress);
            seekBar.setProgress(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
