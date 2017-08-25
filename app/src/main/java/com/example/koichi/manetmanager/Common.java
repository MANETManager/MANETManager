package com.example.koichi.manetmanager;

import android.app.Application;

/**
 * Created by Koichi on 2017/08/16.
 */

public class Common extends Application {
    private double mtod;
    private double mbod;
    private String macAddress;

    public void setMtod(double value){mtod = value;}
    public double getMtod(){return mtod;}

    public void setMbod(double value){mbod = value;}
    public double getMbod(){return mbod;}

    public void setMacAddress(String string){macAddress = string;}
    public String getMacAddress(){return macAddress;}
}
