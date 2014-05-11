package com.ihave.service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class ServletService {

	public static boolean sendRequest(double iLatitu, double iLongi) throws Exception {
		StringBuilder url = new StringBuilder("http://123.75.90.15:8080/VideoNews/ShowRequestServlet");
		url.append('?');
		Map<String,Double> para = new HashMap<String, Double>();
	    para.put("lati", iLatitu);
	    para.put("longi", iLongi);
		for (Entry<String, Double> enty : para.entrySet()) {
			url.append(enty.getKey()).append('=');
			url.append(enty.getValue()).append('&');
		}
		url.deleteCharAt(url.length()-1);
		HttpURLConnection conn = (HttpURLConnection) new URL(url.toString()).openConnection();
		conn.setConnectTimeout(5000);
		if(conn.getResponseCode() == 200){
			System.out.println(url);
			return true;
		}
		return false;
	}


}
