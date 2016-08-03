package com.dingtalk.isv.access.biz.corp.service.impl;

import com.alibaba.fastjson.JSON;
import com.dingtalk.isv.access.api.model.corp.CorpAppVO;
import com.dingtalk.isv.access.api.model.corp.CorpJSAPITicketVO;
import com.dingtalk.isv.access.api.model.corp.CorpTokenVO;
import com.dingtalk.isv.access.api.model.corp.CorpVO;
import com.dingtalk.isv.access.api.model.suite.CorpSuiteAuthVO;
import com.dingtalk.isv.access.api.model.suite.SuiteTokenVO;
import com.dingtalk.isv.access.api.service.corp.CorpManageService;
import com.dingtalk.isv.access.api.service.suite.CorpSuiteAuthService;
import com.dingtalk.isv.access.api.service.suite.SuiteManageService;
import com.dingtalk.isv.access.biz.corp.dao.CorpDao;
import com.dingtalk.isv.access.biz.corp.dao.CorpJSAPITicketDao;
import com.dingtalk.isv.access.biz.corp.dao.CorpTokenDao;
import com.dingtalk.isv.access.biz.corp.model.CorpDO;
import com.dingtalk.isv.access.biz.corp.model.CorpJSAPITicketDO;
import com.dingtalk.isv.access.biz.corp.model.CorpTokenDO;
import com.dingtalk.isv.access.biz.corp.model.helper.CorpConverter;
import com.dingtalk.isv.access.biz.corp.model.helper.CorpJSAPITicketConverter;
import com.dingtalk.isv.access.biz.corp.model.helper.CorpTokenConverter;
import com.dingtalk.isv.access.biz.dingutil.ConfOapiRequestHelper;
import com.dingtalk.isv.access.biz.dingutil.CrmOapiRequestHelper;
import com.dingtalk.isv.access.biz.suite.dao.CorpAppDao;
import com.dingtalk.isv.access.biz.suite.model.CorpAppDO;
import com.dingtalk.isv.access.biz.suite.model.helper.CorpAppConverter;
import com.dingtalk.isv.common.code.ServiceResultCode;
import com.dingtalk.isv.common.log.format.LogFormatter;
import com.dingtalk.isv.common.model.ServiceResult;
import com.dingtalk.open.client.api.model.isv.CorpAuthToken;
import com.dingtalk.open.client.api.service.isv.IsvService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by mint on 16-1-21.
 */
public class CorpManageServiceImpl implements CorpManageService {
    private static final Logger bizLogger = LoggerFactory.getLogger("CORP_MANAGE_LOGGER");
    private static final Logger mainLogger = LoggerFactory.getLogger(CorpManageServiceImpl.class);
    @Autowired
    private CorpDao corpDao;
    @Autowired
    private CorpTokenDao corpTokenDao;
    @Autowired
    private CorpJSAPITicketDao corpJSAPITicketDao;
    @Autowired
    private CorpAppDao corpAppDao;

    @Autowired
    private SuiteManageService suiteManageService;
    @Autowired
    private CorpSuiteAuthService corpSuiteAuthService;
    @Autowired
    private IsvService isvService;
    @Autowired
    private CrmOapiRequestHelper crmOapiRequestHelper;
    @Autowired
    private ConfOapiRequestHelper confOapiRequestHelper;

    @Override
    public ServiceResult<Void> saveOrUpdateCorp(CorpVO corpVO) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpVO", JSON.toJSONString(corpVO))
        ));
        try {
            CorpDO corpDO = CorpConverter.CorpVO2CorpDO(corpVO);
            corpDao.saveOrUpdateCorp(corpDO);
            return ServiceResult.success(null);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("corpVO", corpVO)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("corpVO", corpVO)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }

    /**
     * 维护企业的corptoken
     *
     * @param corpTokenVO
     * @return
     */
    private ServiceResult<Void> saveOrUpdateCorpToken(CorpTokenVO corpTokenVO) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpTokenVO", JSON.toJSONString(corpTokenVO))
        ));
        try {
            CorpTokenDO corpTokenDO = CorpTokenConverter.CorpTokenVO2CorpTokenDO(corpTokenVO);
            corpTokenDao.saveOrUpdateCorpToken(corpTokenDO);
            return ServiceResult.success(null);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("corpTokenVO", JSON.toJSONString(corpTokenVO))
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("corpTokenVO", JSON.toJSONString(corpTokenVO))
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrMsg());
        }

    }

    @Override
    public ServiceResult<CorpTokenVO> getCorpToken(String suiteKey, String corpId) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                LogFormatter.KeyValue.getNew("corpId", corpId)
        ));
        try {
            CorpTokenDO corpTokenDO = corpTokenDao.getCorpToken(suiteKey, corpId);
            //TODO 如果过期重新请求
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.MINUTE, 10);//为了防止误差,提前10分钟更新corptoken
            if (null == corpTokenDO || calendar.getTime().compareTo(corpTokenDO.getExpiredTime()) != -1) {
                ServiceResult<SuiteTokenVO> suiteTokenVOSr = suiteManageService.getSuiteToken(suiteKey);
                String suiteToken = suiteTokenVOSr.getResult().getSuiteToken();
                ServiceResult<CorpSuiteAuthVO> authSr = corpSuiteAuthService.getCorpSuiteAuth(corpId, suiteKey);
                String permanentCode = authSr.getResult().getPermanentCode();
                CorpAuthToken corpAuthToken = isvService.getCorpToken(suiteToken, corpId, permanentCode);
                CorpTokenVO corpTokenVO = new CorpTokenVO();
                corpTokenVO.setCorpId(corpId);
                corpTokenVO.setSuiteKey(suiteKey);
                corpTokenVO.setCorpToken(corpAuthToken.getAccess_token());
                calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.SECOND, (int) corpAuthToken.getExpires_in());
                corpTokenVO.setExpiredTime(calendar.getTime());
                this.saveOrUpdateCorpToken(corpTokenVO);
                corpTokenDO = CorpTokenConverter.CorpTokenVO2CorpTokenDO(corpTokenVO);
            }
            CorpTokenVO corpTokenVO = CorpTokenConverter.CorpTokenDO2CorpTokenVO(corpTokenDO);
            return ServiceResult.success(corpTokenVO);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }

    @Override
    public ServiceResult<CorpTokenVO> deleteCorpToken(String suiteKey, String corpId) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                LogFormatter.KeyValue.getNew("corpId", corpId)
        ));
        try {
            corpTokenDao.deleteCorpToken(suiteKey, corpId);
            return ServiceResult.success(null);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }

    private ServiceResult<Void> saveOrUpdateCorpJSTicket(CorpJSAPITicketVO corpJSTicketVO) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpJSTicketVO", JSON.toJSONString(corpJSTicketVO))
        ));
        try {
            CorpJSAPITicketDO corpJSTicketDO = CorpJSAPITicketConverter.corpJSTicketVO2CorpJSTicketDO(corpJSTicketVO);
            corpJSAPITicketDao.saveOrUpdateCorpJSAPITicket(corpJSTicketDO);
            return ServiceResult.success(null);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("corpJSTicketVO", JSON.toJSONString(corpJSTicketVO))
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("corpJSTicketVO", JSON.toJSONString(corpJSTicketVO))
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }

    @Override
    public ServiceResult<CorpJSAPITicketVO> getCorpJSAPITicket(String suiteKey, String corpId) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                LogFormatter.KeyValue.getNew("corpId", corpId)
        ));
        try {
            CorpJSAPITicketDO corpJSTicketDO = corpJSAPITicketDao.getCorpJSAPITicket(suiteKey, corpId);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.MINUTE, 10);//为了防止误差,提前10分钟更新jsticket
            if (null == corpJSTicketDO || calendar.getTime().compareTo(corpJSTicketDO.getExpiredTime()) != -1) {
                ServiceResult<CorpTokenVO> corpTokenVoSr = this.getCorpToken(suiteKey, corpId);
                ServiceResult<CorpJSAPITicketVO> jsAPITicketSr = confOapiRequestHelper.getJSTicket(suiteKey, corpId, corpTokenVoSr.getResult().getCorpToken());
                bizLogger.info(LogFormatter.getKVLogData(null,
                        LogFormatter.KeyValue.getNew("jsapiticket", JSON.toJSONString(jsAPITicketSr))
                ));
                corpJSTicketDO = CorpJSAPITicketConverter.corpJSTicketVO2CorpJSTicketDO(jsAPITicketSr.getResult());
            }
            CorpJSAPITicketVO corpJSTicketVO = CorpJSAPITicketConverter.corpJSTicketDO2CorpJSTicketVO(corpJSTicketDO);
            this.saveOrUpdateCorpJSTicket(corpJSTicketVO);
            return ServiceResult.success(corpJSTicketVO);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }


    @Override
    public ServiceResult<CorpAppVO> getCorpApp(String corpId, Long appId) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpId", corpId),
                LogFormatter.KeyValue.getNew("appId", appId)
        ));
        try {
            CorpAppDO corpAppDO = corpAppDao.getCorpApp(corpId, appId);
            if (null == corpAppDO) {
                return ServiceResult.success(null);
            }
            CorpAppVO corpAppVO = CorpAppConverter.corpAppDO2CorpAppVO(corpAppDO);
            return ServiceResult.success(corpAppVO);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("appId", appId)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常" + e.toString(),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("appId", appId)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(), ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }
}
