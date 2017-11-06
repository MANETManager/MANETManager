package com.example.koichi.manetmanager;

/**
 * Created by Koichi on 2017/10/30.
 * 端末内に保存されているアカウントを扱うクラス
 * オブジェクトごとに1ユーザーを表す
 */

public class Accounts {
    private String username;
    private String password;
    private double mbod;
    private String macAddress;

    public Accounts(String username, String password){
        this.username = username;
        this.password = password;
    }

    public void setUsername(String s){ username = s; }
    public String getUsername(){ return username; }

    public  void setPassword(String s){ password = s; }
    public String getPassword(){ return password; }

    public void setMbod(double s){ mbod = s; }
    public double getMbod(){ return mbod; }

    public void setMacAddress(String s){ macAddress = s; }
    public String getMacAddress(){ return macAddress; }

}
