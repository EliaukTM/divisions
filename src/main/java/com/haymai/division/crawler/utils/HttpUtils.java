package com.haymai.division.crawler.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

public class HttpUtils {
	private static final String DEFAULT = "default";
	public static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	private static final CloseableHttpClient DEFAULT_CLIENT = getHttpClient();
	private static RequestConfig requestConfig;
	private static final int MAX_TIMEOUT = 300000;
	public static final int CONNCET_SUCCESS = 200;

	static {
		RequestConfig.Builder configBuilder = RequestConfig.custom();
		// 设置连接超时
		configBuilder.setConnectTimeout(MAX_TIMEOUT);
		// 设置读取超时
		configBuilder.setSocketTimeout(MAX_TIMEOUT);
		// 设置从连接池获取连接实例的超时
		configBuilder.setConnectionRequestTimeout(MAX_TIMEOUT);
		// 在提交请求之前 测试连接是否可用
		configBuilder.setStaleConnectionCheckEnabled(true);
		requestConfig = configBuilder.build();

	}

	private HttpUtils() {
	}

	public static String doGet(String url, Map<String, Object> params) {
		return doGet(DEFAULT_CLIENT, url, params);
	}

	/**
	 * 发送 GET 请求（HTTP），K-V形式
	 */
	public static String doGet(CloseableHttpClient httpClient, String url, Map<String, Object> params) {
		String apiUrl = url;
		if (params != null) {
			StringBuilder param = new StringBuilder();
			int i = 0;
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				if (i == 0) {
					param.append("?");
				} else {
					param.append("&");
				}
				param.append(entry.getKey()).append("=").append(params.get(entry.getKey()));
				i++;
			}
			apiUrl += param;
		}

		String result = null;
		CloseableHttpResponse response = null;
		try {
			HttpGet httpGet = new HttpGet(apiUrl);
			response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != CONNCET_SUCCESS) {
				logger.error("request url failed, http code={}, url={}", response.getStatusLine().getStatusCode(), url);
				return null;
			}
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				result = IOUtils.toString(instream, CharEncoding.UTF_8);
				instream.close();
			}
		} catch (Exception e) {
			logger.error("request url exception, url=" + url + ",params=" + params, e);
		} finally {
			try {
				if (response != null) {
					EntityUtils.consume(response.getEntity());
				}
			} catch (Exception e) {
				logger.warn("关闭请求的时候出现了异常 IOException");
			}
		}
		return result;
	}

	public static String doPost(String url, Object params) {
		return doPost(DEFAULT_CLIENT, url, params);
	}

	/**
	 * 发送 POST 请求（HTTP），K-V形式
	 *
	 * @param url
	 * @param params
	 * @return
	 */
	public static String doPost(CloseableHttpClient httpClient, String url, Object params) {
		CloseableHttpResponse response = null;
		String result = null;
		try {
			HttpPost post = new HttpPost(url);
			if (params instanceof Map) {
				// 创建参数列表
				List<NameValuePair> list = new ArrayList<>();
				Map<Object, Object> map = (Map<Object, Object>) params;
				for (Map.Entry entry : map.entrySet()) {
					list.add(new BasicNameValuePair(entry.getKey().toString(), map.get(entry.getKey()).toString()));
				}
				// url格式编码
				UrlEncodedFormEntity uefEntity = new UrlEncodedFormEntity(list, CharEncoding.UTF_8);
				post.setEntity(uefEntity);
			} else if (params instanceof String) {
				post.addHeader("Content-Type", "application/json; charset=utf-8");
				StringEntity strEntity = new StringEntity((String) params, CharEncoding.UTF_8);
				post.setEntity(strEntity);
			} else {
				post.addHeader("Content-Type", "application/json; charset=utf-8");
				StringEntity strEntity = new StringEntity(JSON.toJSONString(params), CharEncoding.UTF_8);
				post.setEntity(strEntity);
			}
			// 执行请求
			response = httpClient.execute(post, new BasicHttpContext());
			// 返回的状态 200 404 等等
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != CONNCET_SUCCESS) {
				logger.debug("request url failed, http code={}, url={}", response.getStatusLine().getStatusCode(), url);
				return null;
			}
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				result = IOUtils.toString(instream, CharEncoding.UTF_8);
			}
			EntityUtils.consume(response.getEntity());
		} catch (Exception e) {
			logger.error("request url exception, url=" + url + ",params=" + params, e);
		} finally {
			try {
				if (response != null) {
					EntityUtils.consume(response.getEntity());
				}

			} catch (Exception e) {
				logger.error("关闭请求的时候出现了异常 IOException", e);
			}
		}
		return result;
	}

	public static CloseableHttpClient getHttpClient() {
		return getHttpClient(DEFAULT);
	}

	/**
	 * HttpClient线程安全 ，建议按业务隔离
	 *
	 * @param bizType
	 * @return
	 */
	public static CloseableHttpClient getHttpClient(String bizType) {
		logger.info("bizType = {}", bizType);
		PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();
		connMgr.setDefaultMaxPerRoute(200);
		return HttpClients.custom().setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
	}
}
