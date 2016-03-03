package com.ppweather.app.util;

/**
 * 解析规则：先按逗号分割，再按竖线分割，将解析出来的数据设置到实体类中，最后调用PPWeatherDB中三个save方法将数据存储到表中
 */
import android.text.TextUtils;

import com.ppweather.app.db.PPWeatherDB;
import com.ppweather.app.model.City;
import com.ppweather.app.model.County;
import com.ppweather.app.model.Province;

public class Utility {

	/**
	 * 解析和处理服务器返回的省数据
	 */
	public synchronized static boolean handleProvinceResponse(PPWeatherDB ppWeatherDB, String response) {
		if (!TextUtils.isEmpty(response)) {
			String[] allProvinces = response.split(",");
			if (allProvinces != null && allProvinces.length > 0) {
				for(String p : allProvinces) {
					String[] array = p.split("\\|");
					Province province = new Province();
					province.setProvinceCode(array[0]);
					province.setProvinceName(array[1]);
					//解析出来的数据存储到Province表
					ppWeatherDB.saveProvince(province);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 解析和处理服务器返回的城市数据
	 */
	public synchronized static boolean handleCityResponse(PPWeatherDB ppWeatherDB, String response, int provinceId) {
		if (!TextUtils.isEmpty(response)) {
			String[] allCities = response.split(",");
			if (allCities != null && allCities.length > 0) {
				for(String c : allCities) {
					String[] array = c.split("\\|");
					City city = new City();
					city.setCityCode(array[0]);
					city.setCityName(array[1]);
					city.setProvinceId(provinceId);
					//解析出来的数据存储到City表
					ppWeatherDB.saveCity(city);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 解析和处理服务器返回的县数据
	 */
	public synchronized static boolean handleCountyResponse(PPWeatherDB ppWeatherDB, String response, int cityId) {
		if (!TextUtils.isEmpty(response)) {
			String[] allCounties = response.split(",");
			if (allCounties != null && allCounties.length > 0) {
				for(String c : allCounties) {
					String[] array = c.split("\\|");
					County county = new County();
					county.setCountyCode(array[0]);
					county.setCountyName(array[1]);
					county.setCityId(cityId);
					//解析出来的数据存储到County表
					ppWeatherDB.saveCounty(county);
				}
				return true;
			}
		}
		return false;
	}
}
