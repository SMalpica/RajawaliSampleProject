package com.android.fitur.rajawalisampleproject;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
