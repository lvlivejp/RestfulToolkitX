package jiux.net.plugin.restful.common;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import jiux.net.plugin.utils.JsonUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class RequestHelper {


    public static String request(String url, String method, Map<String,String> headerMap) throws ClientProtocolException {
        if (method == null) {
            return "method is null";
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        switch (method.toUpperCase()) {
            case "GET":
                return get(url, headerMap);
            case "POST":
                return post(url, headerMap);
            case "PUT":
                return put(url, headerMap);
            case "DELETE":
                return delete(url, headerMap);
            default:
                return "not supported method : " + method + ".";
        }

    }

    public static String get(String url, Map<String,String> headerMap) {
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpMethod = new HttpGet(completed(url));
        String result = null;
        try {

            if (headerMap != null && headerMap.size() > 0) {
                headerMap.forEach(httpMethod::addHeader);
            }

            response = httpClient.execute(httpMethod);
            HttpEntity entity = response.getEntity();
            result = toString(entity);
        } catch (IOException e) {
            result = "There was an error accessing to URL: " + url + "\n\n" + e.toString();
            e.printStackTrace();
        } finally {
            release(response, httpClient);
        }

        return result;
    }

    public static String post(String url, Map<String,String> headerMap) {
        List<BasicNameValuePair> params = new ArrayList<>();

        String result = null;

        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {

            HttpEntity httpEntity;
            httpEntity = new UrlEncodedFormEntity(params, StandardCharsets.UTF_8);
            HttpPost httpMethod = new HttpPost(completed(url));

            if (headerMap != null && headerMap.size() > 0) {
                headerMap.forEach(httpMethod::addHeader);
            }

            httpMethod.setEntity(httpEntity);

            response = httpClient.execute(httpMethod);

            HttpEntity entity = response.getEntity();
            result = toString(entity);
        } catch (IOException e) {
            result = "There was an error accessing to URL: " + url + "\n\n" + e.toString();
            e.printStackTrace();
        } finally {
            release(response, httpClient);
        }

        return result;
    }


    public static String put(String url, Map<String,String> headerMap) throws ClientProtocolException {
        String result;

        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPut httpMethod = new HttpPut(completed(url));

            if (headerMap != null && headerMap.size() > 0) {
                headerMap.forEach(httpMethod::addHeader);
            }

            response = httpClient.execute(httpMethod);

            HttpEntity entity = response.getEntity();
            result = toString(entity);
            // System.out.println(response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            result = "There was an error accessing to URL: " + url + "\n\n" + e.toString();
            e.printStackTrace();
        } finally {
            release(response, httpClient);
        }

        return result;
    }


    public static String delete(String url, Map<String,String> headerMap) throws ClientProtocolException {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        String result;

        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpDelete httpMethod = new HttpDelete(url);

            if (headerMap != null && headerMap.size() > 0) {
                headerMap.forEach(httpMethod::addHeader);
            }

            response = httpClient.execute(httpMethod);

            HttpEntity entity = response.getEntity();
            result = toString(entity);
        } catch (IOException e) {
            result = "There was an error accessing to URL: " + url + "\n\n" + e.toString();
            e.printStackTrace();
        } finally {
            release(response, httpClient);
        }

        return result;
    }


    public static String postRequestBodyWithJson(String url, String json) {
        return postRequestBodyWithJson(url, json, new LinkedHashMap<>());
    }

    public static String postRequestBodyWithJson(String url, String json, Map<String,String> headerMap) {

        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost postMethod = new HttpPost(completed(url));

        String result = null;
        try {
            StringEntity httpEntity = new StringEntity(json,Charset.forName("UTF-8"));

            httpEntity.setContentType("application/json");
            httpEntity.setContentEncoding("UTF-8");

            if (headerMap != null && headerMap.size() > 0) {
                headerMap.forEach(postMethod::addHeader);
            }

            postMethod.addHeader("Content-type", "application/json; charset=utf-8");
            postMethod.setHeader("Accept", "application/json");

//            postMethod.setEntity(new StringEntity(json, Charset.forName("UTF-8")));
            postMethod.setEntity(httpEntity);

            response = httpClient.execute(postMethod);
            result = toString(response.getEntity());

        } catch (IOException e) {
            result = "There was an error accessing to URL: " + url + "\n\n" + e.toString();
        } finally {
            release(response, httpClient);
        }

        return result;
    }

    private static void release(CloseableHttpResponse response, CloseableHttpClient httpClient) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException ignored) {
            }
        }
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String completed(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return url;
    }

    @NotNull
    private static String toString(HttpEntity entity) {
        String result = null;
        try {
            result = EntityUtils.toString(entity, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (result != null && JsonUtils.isValidJson(result)) {
            return JsonUtils.format(result);
        }

        return result;
    }
}
