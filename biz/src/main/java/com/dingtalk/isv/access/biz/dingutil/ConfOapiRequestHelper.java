package com.dingtalk.isv.access.biz.dingutil;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.isv.access.api.model.corp.CorpJSAPITicketVO;
import com.dingtalk.isv.common.code.ServiceResultCode;
import com.dingtalk.isv.common.log.format.LogFormatter;
import com.dingtalk.isv.common.model.ServiceResult;
import com.dingtalk.isv.common.util.HttpRequestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * 开放平台取conf或者token相关http接口封装
 * Created by lifeng.zlf on 2016/4/27.
 */
public class ConfOapiRequestHelper {
    private static Logger logger = LoggerFactory.getLogger(ConfOapiRequestHelper.class);
    private static final Logger bizLogger = LoggerFactory.getLogger("HTTP_INVOKE_LOGGER");
    private HttpRequestHelper httpRequestHelper;
    private String oapiDomain;

    public String getOapiDomain() {
        return oapiDomain;
    }

    public void setOapiDomain(String oapiDomain) {
        this.oapiDomain = oapiDomain;
    }

    public HttpRequestHelper getHttpRequestHelper() {
        return httpRequestHelper;
    }

    public void setHttpRequestHelper(HttpRequestHelper httpRequestHelper) {
        this.httpRequestHelper = httpRequestHelper;
    }

    /**
     * 获取企业的jsapi ticket
     * @param suiteKey
     * @param corpId
     * @param accessToken
     * @return
     */
    public ServiceResult<CorpJSAPITicketVO> getJSTicket(String suiteKey, String corpId, String accessToken) {
        try {
            String url = getOapiDomain() + "/get_jsapi_ticket?access_token=" + accessToken;
            String sr = httpRequestHelper.doHttpGet(url);
            JSONObject jsonObject = JSON.parseObject(sr);
            Long errCode = jsonObject.getLong("errcode");
            if (Long.valueOf(0).equals(errCode)) {
                String ticket = jsonObject.getString("ticket");
                Long expires_in = jsonObject.getLong("expires_in");
                CorpJSAPITicketVO corpJSTicketVO = new CorpJSAPITicketVO();
                corpJSTicketVO.setCorpId(corpId);
                corpJSTicketVO.setSuiteKey(suiteKey);
                corpJSTicketVO.setCorpJSAPITicket(ticket);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.SECOND, expires_in.intValue());
                corpJSTicketVO.setExpiredTime(calendar.getTime());
                return ServiceResult.success(corpJSTicketVO);
            }
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        } catch (IOException e) {
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("accessToken", accessToken)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrCode());
        }
    }
}
