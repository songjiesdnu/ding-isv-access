package  com.dingtalk.isv.access.web.test;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

public class HttpRequestHelper {
    private static final Logger bizLogger  = LoggerFactory.getLogger("SLS_CALL_BACK_LOGGER");

    public static void httpPostJson(URL url, String jsonContent) {
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(url.toString());
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(3000).setConnectTimeout(3000).build();
        httpPost.setConfig(requestConfig);
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity requestEntity = new StringEntity(jsonContent, "utf-8");
        httpPost.setEntity(requestEntity);
        try {
            response = httpClient.execute(httpPost, new BasicHttpContext());
            if (response.getStatusLine().getStatusCode() != 200) {
                return ;
            }
            HttpEntity entity = response.getEntity();
            String resultStr = "";
            if (entity != null) {
                resultStr = EntityUtils.toString(entity, "utf-8");
            }
            return ;
        }
        catch (Exception e) {
            bizLogger.error("http post json failed, url=" + url + ", json=" + jsonContent + ", " + e.getMessage(), e);
            System.out.println("http post json failed, url=" + url + ", json=" + jsonContent + ", " + e.getMessage()+e.toString());
            return ;
        } finally {
            if (response != null) try {
                response.close();
            } catch (IOException e) {
                bizLogger.error(e.getMessage(), e);
            }
        }
        /**
         catch (UnknownHostException e){
         mainLogger.error("http post json failed, url=" + url + ", json=" + jsonContent + ", " + e.getMessage(), e);
         return ServiceResult.getFailureResult(ErrorCodeConstants.SYSTEM_ERROR, "unknown host"+e.toString());
         }catch (ClientProtocolException e){
         mainLogger.error("http post json failed, url=" + url + ", json=" + jsonContent + ", " + e.getMessage(), e);
         return ServiceResult.getFailureResult(ErrorCodeConstants.SYSTEM_ERROR, "unsupport protocol");
         }catch (IOException e){
         mainLogger.error("http post json failed, url=" + url + ", json=" + jsonContent + ", " + e.getMessage(), e);
         return ServiceResult.getFailureResult(ErrorCodeConstants.SYSTEM_ERROR, "connect time out");
         }**/
    }

    public static String getErrorLog(Class<?> clazz,Exception e){
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        StackTraceElement stackTraceElement= e.getStackTrace()[0];// 得到异常棧的首个元素
        String className = stackTraceElement.getClassName();
        String methodName = stackTraceElement.getMethodName();
        int lineNumber = stackTraceElement.getLineNumber();
        return className+"."+methodName+",line:"+lineNumber+",error:"+e.getMessage()+result.toString();
    }



}
