package com.example.koichi.manetmanager;

import android.app.Application;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Koichi on 2017/08/16.
 * アプリ内で(ログインアカウントの)グローバル変数を扱うクラス
 */

public class Common extends Application {
    /** グローバル変数の基本要素 **/
    private String username;
    private String password;
    private double mbod;
    private String macAddress;

    /** グローバル変数の特定・保管用 **/
    private int listIndex; //ログインしているユーザーがaccountGroupのどこに格納されているかを示す
    private int tokenIndex;
    private ArrayList<Accounts> accountGroup;

    /** 基本要素セット・取得 **/
    public void setUsername(String string){ username = string; }
    public String getUsername(){ return username; }

    public void setPassword(String string){ password = string; }
    public String getPassword(){ return password; }

    public void setMbod(double value){ mbod = value; }
    public double getMbod(){ return mbod; }

    public void setMacAddress(String string){ macAddress = string; }
    public String getMacAddress(){ return macAddress; }

    /** グローバル変数特定・保管用関係 **/
    public void setListIndex(int number){ listIndex = number; }
    public int getListIndex(){ return listIndex; }

    public void setTokenIndex(int number){ tokenIndex = number; }
    public int getTokenIndex(){ return tokenIndex; }

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