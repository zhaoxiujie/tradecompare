package com.jd.trade.service;

public class sendpaybijiao {
    int key;
    String yuanshidata;
    String bijiaodata;
    public void setkey(int key)
    {
        this.key=key;
        }
    public void setyuanshidata(String yuanshidata)
    {
        this.yuanshidata=yuanshidata;
    }
    public void setbijiaodata(String bijiaodata)
    {
        this.bijiaodata=bijiaodata;
    }
public int getkey()
    {
        return key;
    }
    public String getyuanshidata()
    {
        return yuanshidata;
    }
    public String getbijiaodata()
    {
        return bijiaodata;
    }
}
