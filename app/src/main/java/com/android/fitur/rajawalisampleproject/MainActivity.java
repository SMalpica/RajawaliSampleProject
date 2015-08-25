package com.android.fitur.rajawalisampleproject;

import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
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

import org.rajawali3d.surface.RajawaliSurfaceView;

import java.util.concurrent.TimeUnit;

/**
 * Author: Sandra Malpica Mallo
 * Date: 17/07/2015.
 * Class: MainActivity.java
 * Comments: main app class, holds main activity. Creates a rajawali surface and sets things up.
 */
public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener{

    Renderer renderer;  //openGL renderer
    RajawaliSurfaceView surface;    //surface
    public static View principal;   //surface
    private int modo=0;
    private boolean tieneGiro = false;
//    private boolean tieneAccel = false;

    /*method called when the activity is created*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        LinearLayout view = (LinearLayout)layoutInflater.inflate(R.layout.player_control, null);
        view.setVerticalGravity(Gravity.BOTTOM);
        viewGroup.addView(view);

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
        //launch a thread to update seekBar progress each second
        new Thread(new Runnable() {
            private int posicion;
            boolean primera = true;
            @Override
            public void run() {
                while(primera || renderer.getMediaPlayer()!=null){
                    while (renderer.getMediaPlayer()==null){}
                    while (!renderer.getMediaPlayer().isPlaying()){}
                    primera=false;
//                    try{
//                        Thread.sleep(2000);
//                    wait(1000);
                        posicion = renderer.getMediaPlayer().getCurrentPosition();
//                    }catch(InterruptedException ex){
//                        return;
//                    }
//                    Log.e("SEEKBAR","posicion "+posicion);
                    seekBar.setMax(renderer.videoLength);
//                    seekBar.setProgress(posicion);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress(posicion);
                        }
                    });
                }
//                Log.e("SEEKBAR","fin");
            }
        }).start();

        PackageManager pm = getPackageManager();
        tieneGiro = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
//        tieneAccel = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
    }

    protected String getAsTime(int t) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toSeconds(t) / 60,
                TimeUnit.MILLISECONDS.toSeconds(t) - TimeUnit.MILLISECONDS.toSeconds(t) / 60 * 60);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        surface.onResume();
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
