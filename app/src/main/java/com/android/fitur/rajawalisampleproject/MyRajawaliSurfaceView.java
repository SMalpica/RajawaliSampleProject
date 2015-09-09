package com.android.fitur.rajawalisampleproject;

import android.content.Context;
import android.util.Log;

import org.rajawali3d.surface.RajawaliSurfaceView;

/**
 * Created by Fitur on 04/09/2015.
 */
public class MyRajawaliSurfaceView extends RajawaliSurfaceView implements Cloneable{

    public MyRajawaliSurfaceView(Context context){
        super(context);
    }

    public MyRajawaliSurfaceView clone(){
//        return this;
        try{
            return (MyRajawaliSurfaceView)super.clone();
        }catch(CloneNotSupportedException ex){
            Log.e("CLONE", "clone not supported");
            return null;
        }

    }
}
