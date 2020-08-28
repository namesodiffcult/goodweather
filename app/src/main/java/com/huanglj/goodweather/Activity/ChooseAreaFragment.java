package com.huanglj.goodweather.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.huanglj.goodweather.R;
import com.huanglj.goodweather.db.City;
import com.huanglj.goodweather.db.County;
import com.huanglj.goodweather.db.Province;
import com.huanglj.goodweather.util.Docs;
import com.huanglj.goodweather.util.HttpUtil;
import com.huanglj.goodweather.util.Utility;

import org.jetbrains.annotations.NotNull;
import org.litepal.LitePal;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.huanglj.goodweather.util.Docs.BASE_URL;
import static com.huanglj.goodweather.util.Docs.LEVEL_CITY;
import static com.huanglj.goodweather.util.Docs.LEVEL_COUNTY;
import static com.huanglj.goodweather.util.Docs.LEVEL_PROVINCE;

public class ChooseAreaFragment extends Fragment {

    private static final String TAG = "ChooseAreaFragment";

    private ProgressDialog mProgressDialog;
    private TextView titleView;
    private Button mBackButton;
    private List<String> dataList = new ArrayList<>();
    private ListView mListView;
    private int currentLevel;
    private ArrayAdapter mAdapter;
    private Province selectedProvince;
    private City selectedCity;
    private List<Province> mProvinceList;
    private List<City> mCityList;
    private List<County> mCountyList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleView = view.findViewById(R.id.title_text);
        mBackButton = view.findViewById(R.id.back_button);
        mListView = view.findViewById(R.id.list_view);
        mAdapter = new ArrayAdapter(getContext(),android.R.layout.simple_list_item_1,dataList);
        mListView.setAdapter( mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = mProvinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = mCityList.get(position);
                    queryCounties();
                }
            }
        });
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询市内所有的县
     */
    private void queryCounties() {
        titleView.setText(selectedCity.getCityName());
        mBackButton.setVisibility(View.VISIBLE);
        mCountyList = LitePal.where("cityid = ? ", String.valueOf(selectedCity.getId())).find(County.class);
        if(mCountyList.size() > 0){
            dataList.clear();
            for (County county : mCountyList) {
                dataList.add(county.getCountyName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = Docs.LEVEL_COUNTY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = BASE_URL + "/api/china/" + provinceCode + "/" + cityCode;
            queryFormServer(address,"county");
        }
    }

    /**
     * 查询省内所有的市
     */
    private void queryCities() {
        titleView.setText(selectedProvince.getProvinceName());
        mBackButton.setVisibility(View.VISIBLE);
        mCityList = LitePal.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if (mCityList.size() > 0){
            dataList.clear();
            for (City city : mCityList) {
                dataList.add(city.getCityName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = Docs.BASE_URL + "/api/china/" + provinceCode;
            queryFormServer(address,"city");
        }
    }

    /**
     * 查询中国所有的省
     */
    private void queryProvinces() {
        titleView.setText("中国");
        mBackButton.setVisibility(View.GONE);
        mProvinceList = LitePal.findAll(Province.class);
        if(mProvinceList.size() > 0){
            dataList.clear();
            for (Province province : mProvinceList) {
                dataList.add(province.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
            //设置默认选中数据为第一个
            mListView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            String address = Docs.BASE_URL + "/api/china";
            queryFormServer(address,"province");
        }
    }

    /**
     * 根据地址在服务器上查询所有的市县数据
     * @param address
     * @param type
     */
    private void queryFormServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);

                }
                if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());

                }
                if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,
                            selectedCity.getId());

                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    private void closeProgressDialog() {
        if(mProgressDialog!= null){
            mProgressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if(mProgressDialog == null){
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }
}
