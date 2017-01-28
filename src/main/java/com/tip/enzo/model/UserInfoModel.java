package com.tip.enzo.model;

import java.io.Serializable;

/**
 * Created by enzo on 17/1/28.
 */
public class UserInfoModel implements Serializable {
    private Integer cardNum;
    private String diamond;
    private String nickName;
    private String playerType;


    public Integer getCardNum() {
        return cardNum;
    }

    public void setCardNum(Integer cardNum) {
        this.cardNum = cardNum;
    }

    public String getDiamond() {
        return diamond;
    }

    public void setDiamond(String diamond) {
        this.diamond = diamond;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPlayerType() {
        return playerType;
    }

    public void setPlayerType(String playerType) {
        this.playerType = playerType;
    }
}
