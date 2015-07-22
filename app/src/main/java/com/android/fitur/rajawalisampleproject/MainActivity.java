package com.android.fitur.rajawalisampleproject;

import android.app.ActionBar;
import android.app.Activity;
import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;

import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;


//public class MainActivity extends Activity implements SurfaceHolder.Callback{
public class MainActivity extends Activity{
    Renderer renderer;
    RajawaliSurfaceView surface;
    public static View principal;
//    MediaPlayer player;
//    private SurfaceHolder holder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surface = new RajawaliSurfaceView(this);
        surface.setFrameRate(60.0);
//        surface.setRenderMode(IRajawaliSurface.RENDERMODE_WHEN_DIRTY);
        principal = surface;
        // Add mSurface to your root view
        addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));

        /*player = MediaPlayer.create(this,R.raw.pyrex);
        player.setLooping(true);
        holder = surface.getHolder();
        holder.addCallback(this);*/
//        renderer = new Renderer(this,player);
        renderer = new Renderer(this);
        surface.setSurfaceRenderer(renderer);
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

/*    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        player.setDisplay(surface.getHolder());
//        player.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub

    }*/

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
