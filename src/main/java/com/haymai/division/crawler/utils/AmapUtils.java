package com.haymai.division.crawler.utils;

import static com.haymai.division.crawler.McaCrawler.SINGLE_LINE_FORMAT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Description:
 *
 * @author yuanma
 * @version 1.0
 * @date 6/17/2019 3:49 PM
 * @since JDK 1.8
 */
public class AmapUtils {

	public static List<String> handleExtractDivision(String amapKey, List<String> codes) {
		List<String> result = new ArrayList<>();
		for (String code : codes) {
			Map<String, Object> params = new HashMap<>(4);
			params.put("key", amapKey);
			params.put("keywords", code);
			String queryResults = HttpUtils.doGet("http://restapi.amap.com/v3/config/district", params);
			JSONObject jsonObject = JSONObject.parseObject(queryResults);
			JSONObject queryDistrict = (JSONObject) jsonObject.getJSONArray("districts").get(0);
			if (code.endsWith("0000")) {
				result.add(String.format(SINGLE_LINE_FORMAT, code.substring(0, 2) + "0100",
						queryDistrict.getString("name")));
			}
			JSONArray districts = queryDistrict.getJSONArray("districts");
			for (int i = 0; i < districts.size(); i++) {
				JSONObject districtJSONObject = (JSONObject) districts.get(i);
				String adCode = districtJSONObject.getString("adcode");
				String name = districtJSONObject.getString("name");
				if (adCode.equals(code)) {
					adCode = String.valueOf(Integer.valueOf(adCode) + i + 1);
				}
                result.add(String.format(SINGLE_LINE_FORMAT, adCode,
                        name));
			}
		}
		return result;
	}
}
