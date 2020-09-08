package com.huanglj.goodweather.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {
    //把接口返回的city命名为cityname
    @SerializedName("city")
    public String cityName;
    @SerializedName("id")
    public String weatherId;
    public Update update;
    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
