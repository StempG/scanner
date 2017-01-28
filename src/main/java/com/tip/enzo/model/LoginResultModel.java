package com.tip.enzo.model;

import java.io.Serializable;

public class LoginResultModel implements Serializable {


    private String result;

    private String isUpdate;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getIsUpdate() {
        return isUpdate;
    }

    public void setIsUpdate(String isUpdate) {
        this.isUpdate = isUpdate;
    }
}
