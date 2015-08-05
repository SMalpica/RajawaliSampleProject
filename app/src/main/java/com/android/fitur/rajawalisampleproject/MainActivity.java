package com.android.fitur.rajawalisampleproject;

import android.app.ActionBar;
import android.app.Activity;
import android.media.Image;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;

import org.rajawali3d.surface.RajawaliSurfaceView;

/**
 * Author: Sandra Malpica Mallo
 * Date: 17/07/2015.
 * Class: MainActivity.java
 * Comments: main app class, holds main activity. Creates a rajawali surface and sets things up.
 */
public class MainActivity extends Activity{

    Renderer renderer;  //openGL renderer
    RajawaliSurfaceView surface;    //surface
    public static View principal;   //surface

    /*method called when the activity is created*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //creates the surface and sets the frameRate
        surface = new RajawaliSurfaceView(this);
        surface.setFrameRate(60.0);
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
        ImageButton modeButton = (ImageButton) view.findViewById(R.id.modebutton);
//        playButton.setMinimumHeight(backButton.getHeight());
//        modeButton.setMinimumHeight(backButton.getHeight());
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(renderer.getMediaPlayer().isPlaying()){
                    renderer.getMediaPlayer().pause();
                    playButton.setImageLevel(1);
//                    playButton.refreshDrawableState();
//                    playButton.setImageResource(R.drawable.playback_play);
//                    playButton.setBackgroundResource(0);
//                    playButton.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.playback_play));
                }else{
                    renderer.getMediaPlayer().start();
                    playButton.setImageLevel(0);
//                    playButton.setImageResource(R.drawable.playback_pause);
//                    playButton.setImageDrawable(getApplicationContext().getResources().getDrawable(R.drawable.playback_pause));
                }
            }
        });

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
}
