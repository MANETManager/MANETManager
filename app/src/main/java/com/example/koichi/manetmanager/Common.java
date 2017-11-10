package com.example.koichi.manetmanager;

import android.app.Application;

import java.util.ArrayList;

/**
 * Created by Koichi on 2017/08/16.
 * アプリ内で(ログインアカウントの)グローバル変数を扱うクラス
 */

//TODO: Cトークン関係のグローバル変数やメソッド、init()への初期値を追加

public class Common extends Application {
    /** グローバル変数の基本要素 **/
    private String username;
    private String password;
    private double mbod;
    private String macAddress;

    /** グローバル変数の特定・保管用 **/
    private int listIndex; //ログインしているユーザーがaccountGroupのどこに格納されているかを示す
    private ArrayList<Accounts> accountGroup;

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

    void init(){
        username = null;
        password = null;
        mbod = 0;
        macAddress = null;
        listIndex = 0;
    }

}
