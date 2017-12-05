package com.example.koichi.manetmanager;

import java.util.ArrayList;

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

    /** IDに対応したコミュニティトークン要素 **/
    private String groupId;
    private String groupName;
    private String tokenId;
    private String tokenMbod;
    private String sourceAddress; //ソースノードのmacAddress
    private String postId;

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

    /** コミュティトークン要素セット・取得 **/
    public void setGroupId(String string){ groupId = string; }
    public String getGroupId(){ return groupId; }

    public void setGroupName(String string){ groupName = string; }
    public String getGroupName(){ return groupName; }

    public void setTokenId(String string){ tokenId = string; }
    public String getTokenId(){ return tokenId; }

    public void setTokenMbod(String string){ tokenMbod = string; }
    public String getTokenMbod(){ return tokenMbod; }

    public void setSourceAddress(String string){ sourceAddress = string; }
    public String getSourceAddress(){ return sourceAddress; }

    public void setPostId(String string){ postId = string; }
    public String getPostId(){ return postId; }

    void initToken(){
        username = null;
        password = null;
        mbod = 0;
        macAddress = null;
    }
}