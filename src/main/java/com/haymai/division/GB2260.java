package com.haymai.division;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class GB2260 {
	private HashMap<String, String> data;
	private ArrayList<Division> provinces;

	public GB2260() {
		data = new HashMap<>();
		provinces = new ArrayList<>();
		InputStream inputStream = DivisionFileUtils.getInputStream();
		BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
		try {
			while (r.ready()) {
				String line = r.readLine();
				String[] split = line.split("\t");
				String code = split[0];
				String name = split[1];

				data.put(code, name);

				if (Pattern.matches("^\\d{2}0{4}$", code)) {
					Division division = new Division();
					division.setCode(code);
					division.setName(name);
					provinces.add(division);
				}
			}
		} catch (IOException e) {
			try {
				r.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			throw new RuntimeException(e);
		}
	}

	public Division getDivision(String code) {
		if (code.length() != 6) {
			throw new InvalidCodeException("Invalid code");
		}

		if (!data.containsKey(code)) {
			return null;
		}

		Division division = new Division();
		division.setName(data.get(code));
		division.setCode(code);

		if (Pattern.matches("^\\d{2}0{4}$", code)) {
			return division;
		}

		String provinceCode = code.substring(0, 2) + "0000";
		division.setProvinceCode(provinceCode);
		division.setProvince(data.get(provinceCode));

		if (Pattern.matches("^\\d{4}0{2}$", code)) {
			return division;
		}

		String prefectureCode = code.substring(0, 4) + "00";
		division.setPrefectureCode(prefectureCode);
		division.setPrefecture(data.get(prefectureCode));

		return division;
	}

	public List<Division> getProvinces() {
		return provinces;
	}

	public List<Division> getPrefectures(String code) {
		List<Division> rv = new ArrayList<>();

		if (!Pattern.matches("^\\d{2}0{4}$", code)) {
			throw new InvalidCodeException("Invalid province code");
		}

		if (!data.containsKey(code)) {
			throw new InvalidCodeException("Province code not found");
		}

		Division province = getDivision(code);

		Pattern pattern = Pattern.compile("^" + code.substring(0, 2) + "\\d{2}00$");
		for (String key : data.keySet()) {
			if (pattern.matcher(key).matches() && !key.endsWith("0000")) {
				Division division = getDivision(key);
				division.setProvince(province.getName());
				rv.add(division);
			}
		}

		return rv;
	}

	public List<Division> getCounties(String code) {
		List<Division> rv = new ArrayList<>();

		if (!Pattern.matches("^\\d+[1-9]0{2,3}$", code)) {
			throw new InvalidCodeException("Invalid prefecture code");
		}

		if (!data.containsKey(code)) {
			throw new InvalidCodeException("Prefecture code not found");
		}

		Division prefecture = getDivision(code);
		Division province = getDivision(code.substring(0, 2) + "0000");

		Pattern pattern = Pattern.compile("^" + code.substring(0, 4) + "\\d+$");
		for (String key : data.keySet()) {
			if (pattern.matcher(key).matches()) {
				Division division = getDivision(key);
				division.setProvince(province.getName());
				division.setPrefecture(prefecture.getName());
				rv.add(division);
			}
		}

		return rv;
	}

	public Set<String> getAllDivisionCodes() {
		return this.data.keySet();
	}
}
