package com.huanglj.goodweather.API;

import com.huanglj.goodweather.beans.JsProvince;

import retrofit2.Call;
import retrofit2.http.GET;

public interface API {

    @GET("/api/china")
    Call<JsProvince> getProvince();

}
