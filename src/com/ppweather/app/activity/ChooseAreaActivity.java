package com.ppweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import com.ppweather.app.R;
import com.ppweather.app.db.PPWeatherDB;
import com.ppweather.app.model.City;
import com.ppweather.app.model.County;
import com.ppweather.app.model.Province;
import com.ppweather.app.util.HttpCallbackListener;
import com.ppweather.app.util.HttpUtil;
import com.ppweather.app.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity{

	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private PPWeatherDB ppWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	
	/**
	 * 省列表
	 */
	private List<Province> provinceList;
	/**
	 * 市列表
	 */
	private List<City> cityList;
	/**
	 * 县列表
	 */
	private List<County> countyList;
	
	/**
	 * 选中的省份
	 */
	private Province selectedProvince;
	/**
	 * 选中的城市
	 */
	private City selectedCity;
	/**
	 * 选中的县
	 */
	private County selectedCounty;
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		//获取控件实例
		listView = (ListView)findViewById(R.id.list_view);
		titleText = (TextView)findViewById(R.id.title_text);
		//初始化ArrayAdapter
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,dataList);
		//将ArrayAdapter设置为listView的适配器
		listView.setAdapter(adapter);
		//获取PPWeatherDB实例
		ppWeatherDB = PPWeatherDB.getInstance(this);
		//设置点击事件
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				// TODO Auto-generated method stub
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(index);
					queryCities();
				} else if(currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(index);
					queryCounties();
				}
			}
		});
		queryProvinces();
	}
	
	/**
	 * 查询全国所有的省，优先从数据库查找，没有再去服务器上查询
	 */
	private void queryProvinces() {
		//先调用ppWeatherDB的loadProvinces方法来从数据库读取数据
		provinceList = ppWeatherDB.loadProvinces();
		//如果读取到，就显示在界面上
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		} else {
			//没读取到，就从服务器读取
			queryFromServer(null, "province");
		}
	}
	
	/**
	 * 查询选中省内所有城市，优先从数据库查找，没有再去服务器上查询
	 */
	private void queryCities() {
		cityList = ppWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}
	
	/**
	 * 查询选中市内所有县，优先从数据库查找，没有再去服务器上查询
	 */
	private void queryCounties() {
		countyList = ppWeatherDB.loadCounties(selectedCity.getId());
		if (countyList.size() > 0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}
	
	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据
	 */
	private void queryFromServer(final String code, final String type) {
		String address;
		//根据传入的参数拼接查询地址
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		
		//确定地址后调用HttpUtil的sendHttpRequest方法向服务器发送请求
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			
			@Override
			public void onFinish(String response) {
				// 响应的数据会回调到onFinish方法中
				boolean result = false;
				//调用Utility的handleProvinceResponse方法来解析和处理服务器返回的数据，并存储到数据库中
				if ("province".equals(type)) {
					result = Utility.handleProvinceResponse(ppWeatherDB, response);
				} else if("city".equals(type)) {
					result = Utility.handleCityResponse(ppWeatherDB, response, selectedProvince.getId());
				} else if("county".equals(type)) {
					result = Utility.handleCountyResponse(ppWeatherDB, response, selectedCity.getId());
				}
				
				if (result) {
					//解析完成后，再次调用queryProvinces()方法，重新加载省资料
					//queryProvinces()牵涉到UI操作，必须要在主线程调用
					//这里借助runOnUiThread()方法来实现从子线程切换到主线程
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							} else if("city".equals(type)) {
								queryCities();
							} else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		// TODO Auto-generated method stub
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	/**
	 * 捕获back键，根据当前级别判断，此时是返回上一级列表还是直接退出
	 */
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if(currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			finish();
		}
	}
}
