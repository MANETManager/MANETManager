package com.example.koichi.manetmanager;

import android.app.Application;

import java.util.ArrayList;

/**
 * Created by Koichi on 2017/08/16.
 * アプリ内で(ログインアカウントの)グローバル変数を扱うクラス
 */

public class Common extends Application {
    private String username;
    private String password;
    private double mbod;
    private String macAddress;
    private int listIndex; //ログインしているユーザーがaccountGroupのどこに格納されているかを示す
    private ArrayList<Accounts> accountGroup = new ArrayList<Accounts>();

    public void setUsername(String string){ username = string; }
    public String getUsername(){ return username; }

    public void setPassword(String string){ password = string; }
    public String getPassword(){ return password; }

    public void setMbod(double value){ mbod = value; }
    public double getMbod(){ return mbod; }

    public void setMacAddress(String string){ macAddress = string; }
    public String getMacAddress(){ return macAddress; }

    public void setListIndex(int number){ listIndex = number; }
    public int getListIndex(){ return listIndex; }

    public void setAccountGroup(ArrayList<Accounts> list){ accountGroup = list; }
    public ArrayList<Accounts> getAccountGroup(){ return accountGroup; }

}
