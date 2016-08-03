package com.dingtalk.isv.access.biz.suite.service.impl;

import com.alibaba.fastjson.JSON;
import com.dingtalk.isv.access.api.constant.AccessSystemConfig;
import com.dingtalk.isv.access.api.model.corp.CorpAppVO;
import com.dingtalk.isv.access.api.model.corp.CorpTokenVO;
import com.dingtalk.isv.access.api.model.corp.CorpVO;
import com.dingtalk.isv.access.api.model.event.AuthChangeEvent;
import com.dingtalk.isv.access.api.model.event.CorpAuthSuiteEvent;
import com.dingtalk.isv.access.api.model.event.mq.CorpAuthSuiteMessage;
import com.dingtalk.isv.access.api.model.event.mq.SuiteCallBackMessage;
import com.dingtalk.isv.access.api.model.suite.CorpSuiteAuthVO;
import com.dingtalk.isv.access.api.model.suite.CorpSuiteCallBackVO;
import com.dingtalk.isv.access.api.model.suite.SuiteTokenVO;
import com.dingtalk.isv.access.api.model.suite.SuiteVO;
import com.dingtalk.isv.access.api.service.corp.CorpManageService;
import com.dingtalk.isv.access.api.service.suite.CorpSuiteAuthService;
import com.dingtalk.isv.access.api.service.suite.SuiteManageService;
import com.dingtalk.isv.access.biz.dingutil.CrmOapiRequestHelper;
import com.dingtalk.isv.access.biz.suite.dao.AppDao;
import com.dingtalk.isv.access.biz.suite.dao.CorpAppDao;
import com.dingtalk.isv.access.biz.suite.dao.CorpSuiteAuthDao;
import com.dingtalk.isv.access.biz.suite.dao.CorpSuiteAuthFaileDao;
import com.dingtalk.isv.access.biz.suite.model.AppDO;
import com.dingtalk.isv.access.biz.suite.model.CorpAppDO;
import com.dingtalk.isv.access.biz.suite.model.CorpSuiteAuthDO;
import com.dingtalk.isv.access.biz.suite.model.helper.CorpAppConverter;
import com.dingtalk.isv.access.biz.suite.model.helper.CorpSuiteAuthConverter;

import com.dingtalk.isv.common.code.ServiceResultCode;
import com.dingtalk.isv.common.log.format.LogFormatter;
import com.dingtalk.isv.common.model.ServiceResult;
import com.dingtalk.open.client.api.model.isv.CorpAuthInfo;
import com.dingtalk.open.client.api.model.isv.CorpAuthSuiteCode;
import com.dingtalk.open.client.api.service.isv.IsvService;
import com.dingtalk.open.client.common.ServiceException;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Queue;
import java.util.List;

/**
 * Created by lifeng.zlf on 2016/1/19.
 */
public class CorpSuiteAuthServiceImpl implements CorpSuiteAuthService {
    private static final Logger bizLogger = LoggerFactory.getLogger("CORP_SUITE_AUTH_LOGGER");
    private static final Logger mainLogger = LoggerFactory.getLogger(CorpSuiteAuthServiceImpl.class);

    @Autowired
    private CorpSuiteAuthDao corpSuiteAuthDao;
    @Autowired
    private CorpAppDao corpAppDao;
    @Autowired
    private AppDao appDao;
    @Autowired
    private CorpSuiteAuthFaileDao corpSuiteAuthFaileDao;
    @Autowired
    private SuiteManageService suiteManageService;
    @Autowired
    private CorpManageService corpManageService;
    @Autowired
    private IsvService isvService;
    @Autowired
    private CrmOapiRequestHelper crmOapiRequestHelper;
    @Autowired
    private EventBus corpAuthSuiteEventBus;
    @Autowired
    private AccessSystemConfig accessSystemConfig;
    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    @Qualifier("orgAuthSuiteQueue")
    private Queue orgAuthSuiteQueue;

    @Override
    public ServiceResult<CorpSuiteAuthVO> getCorpSuiteAuth(String corpId, String suiteKey) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpId", corpId),
                LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
        ));
        try {
            CorpSuiteAuthDO corpSuiteAuthDO = corpSuiteAuthDao.getCorpSuiteAuth(corpId, suiteKey);
            if (null == corpSuiteAuthDO) {
                bizLogger.warn(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                        "授权关系未找到",
                        LogFormatter.KeyValue.getNew("corpId", corpId),
                        LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
                ));
                return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
            }
            CorpSuiteAuthVO corpSuiteAuthVO = CorpSuiteAuthConverter.CorpSuiteAuthDO2CorpSuiteAuthVO(corpSuiteAuthDO);
            return ServiceResult.success(corpSuiteAuthVO);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }

    @Override
    public ServiceResult<Void> saveOrUpdateCorpSuiteAuth(CorpSuiteAuthVO corpSuiteAuthVO) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpSuiteAuthVO", JSON.toJSONString(corpSuiteAuthVO))
        ));
        try {
            CorpSuiteAuthDO corpSuiteAuthDO = CorpSuiteAuthConverter.CorpSuiteAuthVO2CorpSuiteAuthDO(corpSuiteAuthVO);
            corpSuiteAuthDao.addOrUpdateCorpSuiteAuth(corpSuiteAuthDO);
            return ServiceResult.success(null);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("corpSuiteAuthVO", JSON.toJSONString(corpSuiteAuthVO))
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("corpSuiteAuthVO", JSON.toJSONString(corpSuiteAuthVO))
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }

    @Override
    public ServiceResult<Void> deleteCorpSuiteAuth(String corpId, String suiteKey) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpId", corpId),
                LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
        ));
        try {
            corpSuiteAuthDao.deleteCorpSuiteAuth(corpId, suiteKey);
            return ServiceResult.success(null);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }


    @Override
    public ServiceResult<Void> saveOrUpdateCorpApp(CorpAppVO corpAppVO) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpAppVO", JSON.toJSONString(corpAppVO))
        ));
        CorpAppDO corpAppDO = CorpAppConverter.corpAppVO2CorpAppDO(corpAppVO);
        corpAppDao.saveOrUpdateCorpApp(corpAppDO);
        return ServiceResult.success(null);
    }

    @Override
    public ServiceResult<CorpAppVO> getCorpApp(String corpId, Long appId) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpId", corpId),
                LogFormatter.KeyValue.getNew("appId", appId)
        ));
        CorpAppDO corpAppDO = corpAppDao.getCorpApp(corpId, appId);
        if (null == corpAppDO) {
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
        CorpAppVO corpAppVO = CorpAppConverter.corpAppDO2CorpAppVO(corpAppDO);
        return ServiceResult.success(corpAppVO);
    }

    @Override
    public ServiceResult<Void> deleteCorpApp(String corpId, Long appId) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("corpId", corpId),
                LogFormatter.KeyValue.getNew("appId", appId)
        ));
        corpAppDao.deleteCorpApp(corpId, appId);
        return ServiceResult.success(null);
    }

    @Override
    public ServiceResult<CorpSuiteAuthVO> saveOrUpdateCorpSuiteAuth(String suiteKey, String tmpAuthCode) {
        try {
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("tmpAuthCode", tmpAuthCode),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
            ));
            ServiceResult<SuiteTokenVO> suiteTokenSr = suiteManageService.getSuiteToken(suiteKey);
            String suiteToken = suiteTokenSr.getResult().getSuiteToken();
            CorpAuthSuiteCode corpAuthSuiteCode = isvService.getPermanentCode(suiteToken, tmpAuthCode);
            String corpId = corpAuthSuiteCode.getAuth_corp_info().getCorpid();
            String permanentCode = corpAuthSuiteCode.getPermanent_code();
            CorpSuiteAuthVO corpSuiteAuthVO = new CorpSuiteAuthVO();
            corpSuiteAuthVO.setCorpId(corpId);
            corpSuiteAuthVO.setPermanentCode(permanentCode);
            corpSuiteAuthVO.setSuiteKey(suiteKey);
            ServiceResult<Void> saveAuthSr = this.saveOrUpdateCorpSuiteAuth(corpSuiteAuthVO);
            if (!saveAuthSr.isSuccess()) {
                bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                        "存储企业对套件授权信息失败",
                        saveAuthSr.getCode(),
                        saveAuthSr.getMessage(),
                        LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                        LogFormatter.KeyValue.getNew("tmpAuthCode", tmpAuthCode)
                ));
                return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
            }
            //异步逻辑
            CorpAuthSuiteEvent corpAuthSuiteEvent = new CorpAuthSuiteEvent();
            corpAuthSuiteEvent.setSuiteKey(suiteKey);
            corpAuthSuiteEvent.setSuiteToken(suiteToken);
            corpAuthSuiteEvent.setCorpId(corpId);
            corpAuthSuiteEvent.setPermanentCode(permanentCode);
            corpAuthSuiteEventBus.post(corpAuthSuiteEvent);
            return ServiceResult.success(corpSuiteAuthVO);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("tmpAuthCode", tmpAuthCode)
            ),e);
            if (e instanceof com.dingtalk.open.client.common.ServiceException) {
                if (40078 == ((ServiceException) e).getCode()) {
                    //不存在的临时授权码,不再继续重试
                    return ServiceResult.success(null);
                }
            }
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }

    @Override
    public ServiceResult<Void> activeCorpApp(String suiteKey, String corpId,String permanentCode) {
        try{
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("permanentCode", permanentCode)
            ));
            ServiceResult<SuiteTokenVO> suiteTokenSr = suiteManageService.getSuiteToken(suiteKey);
            //1.获取套件token
            if(!suiteTokenSr.isSuccess()){
                return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
            }
            String suiteToken = suiteTokenSr.getResult().getSuiteToken();
            //2.激活
            try {
                isvService.activateSuite(suiteToken, suiteKey, corpId);
            } catch (Exception e) {
                if (e instanceof com.dingtalk.open.client.common.ServiceException) {
                    if (41030 == ((ServiceException) e).getCode()) {
                        //企业未对该套件授权,终止,返回成功
                        return ServiceResult.success(null);
                    }
                }else{
                    return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
                }
            }
            //3.更新企业信息
            ServiceResult<Void> getCorpInfoSr = this.getCorpInfo(suiteToken,suiteKey,corpId, permanentCode);
            if(!getCorpInfoSr.isSuccess()){
                return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
            }
            //4.注册或者更新回调
            ServiceResult<Void> saveCallBackSr = this.saveCorpCallback(suiteKey, corpId, (accessSystemConfig.getCorpSuiteCallBackUrl() + suiteKey), SuiteCallBackMessage.Tag.getAllTag());
            if(!saveCallBackSr.isSuccess()){
                return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
            }
            //5.发送mq到各个业务方,告知一个企业对讨价授权了,业务方自己去做对应的业务
            jmsTemplate.send(orgAuthSuiteQueue,new CorpAuthSuiteMessage(corpId,suiteKey, CorpAuthSuiteMessage.Tag.Auth));
            return ServiceResult.success(null);
        }catch (Exception e){
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("permanentCode", permanentCode)
            ));
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }

    }

    @Override
    public ServiceResult<Void> getCorpInfo(String suiteToken, String suiteKey, String corpId, String permanentCode) {
        try {
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("permanentCode", permanentCode)
            ));
            CorpAuthInfo corpAuthInfo = isvService.getAuthInfo(suiteToken, suiteKey, corpId, permanentCode);
            CorpVO corpVO = new CorpVO();
            corpVO.setCorpId(corpId);
            corpVO.setCorpLogoUrl(corpAuthInfo.getAuth_corp_info().getCorp_logo_url());
            corpVO.setCorpName(corpAuthInfo.getAuth_corp_info().getCorp_name());
            corpVO.setIndustry(corpAuthInfo.getAuth_corp_info().getIndustry());
            corpVO.setInviteCode(corpAuthInfo.getAuth_corp_info().getInvite_code());
            corpVO.setInviteUrl(corpAuthInfo.getAuth_corp_info().getInvite_url());
            ServiceResult<Void> addCorpSr = corpManageService.saveOrUpdateCorp(corpVO);
            if(!addCorpSr.isSuccess()){
                return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
            }
            List<CorpAuthInfo.Agent> agentList = corpAuthInfo.getAuth_info().getAgent();
            for (CorpAuthInfo.Agent agent : agentList) {
                CorpAppVO corpAppVO = new CorpAppVO();
                corpAppVO.setCorpId(corpId);
                corpAppVO.setAgentId(agent.getAgentid());
                corpAppVO.setAgentName(agent.getAgent_name());
                corpAppVO.setLogoUrl(agent.getLogo_url());
                corpAppVO.setAppId(agent.getAppid());
                this.saveOrUpdateCorpApp(corpAppVO);
            }
            return ServiceResult.success(null);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("suiteToken", suiteToken),
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("permanentCode", permanentCode)
            ),e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }

    @Override
    public ServiceResult<Void> handleChangeAuth(String suiteKey, String corpId) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                LogFormatter.KeyValue.getNew("corpId", corpId)
        ));
        String suiteToken = "";
        String permanentCode = "";
        try {
            //1.获取suiteToken
            ServiceResult<SuiteTokenVO> suiteTokenSr = suiteManageService.getSuiteToken(suiteKey);
            ServiceResult<CorpSuiteAuthVO> corpSuiteAuthSr = this.getCorpSuiteAuth(corpId, suiteKey);
            if (!corpSuiteAuthSr.isSuccess() || null == corpSuiteAuthSr.getResult()) {
                bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                        corpSuiteAuthSr.getCode(),
                        corpSuiteAuthSr.getMessage(),
                        "授权关系不存在或者已经解除",
                        LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                        LogFormatter.KeyValue.getNew("corpId", corpId)
                ));
                return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
            }
            suiteToken = suiteTokenSr.getResult().getSuiteToken();
            permanentCode = corpSuiteAuthSr.getResult().getPermanentCode();
        } catch (Exception e) {
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
        //异步逻辑
        AuthChangeEvent authChangeEvent = new AuthChangeEvent();
        authChangeEvent.setSuiteKey(suiteKey);
        authChangeEvent.setSuiteToken(suiteToken);
        authChangeEvent.setCorpId(corpId);
        authChangeEvent.setPermanentCode(permanentCode);
        corpAuthSuiteEventBus.post(authChangeEvent);
        return ServiceResult.success(null);
    }


    @Override
    public ServiceResult<Void> handleRelieveAuth(String suiteKey, String corpId) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                LogFormatter.KeyValue.getNew("corpId", corpId)
        ));
        //ServiceResult<CorpTokenVO> corpTokenSr = corpManageService.getCorpToken(suiteKey, corpId);
        //删除企业回调,因为解除授权了,所以这个回调用的token已经不可以使用了
        //crmOapiRequestHelper.deleteCorpSuiteCallback(suiteKey,corpId,corpTokenSr.getResult().getCorpToken());

        ServiceResult<Void> deleteAuthSr = this.deleteCorpSuiteAuth(corpId, suiteKey);
        List<AppDO> appList = appDao.getAppBySuiteKey(suiteKey);
        for (AppDO appDO : appList) {
            //TODO
            ServiceResult<Void> deleteCorpAppSr = this.deleteCorpApp(corpId, appDO.getAppId());
        }
        //删除企业token
        corpManageService.deleteCorpToken(suiteKey, corpId);

        return ServiceResult.success(null);
    }

    @Override
    public ServiceResult<List<CorpSuiteAuthVO>> getCorpSuiteAuthByPage(String suiteKey, int startRow, int pageSize) {
        bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                LogFormatter.KeyValue.getNew("startRow", startRow),
                LogFormatter.KeyValue.getNew("pageSize", pageSize)
        ));
        System.out.println(suiteKey + "|" + startRow + "|" + pageSize);
        try {
            List<CorpSuiteAuthDO> list = corpSuiteAuthDao.getCorpSuiteAuthByPage(suiteKey, startRow, pageSize);
            System.out.println(JSON.toJSONString(list));
            if (null == list || list.isEmpty()) {
                bizLogger.warn(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                        "该套件没有被授权给任何企业",
                        LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
                ));
                return ServiceResult.success(null);
            }
            List<CorpSuiteAuthVO> resultList = Lists.newArrayList();
            for (CorpSuiteAuthDO item : list) {
                resultList.add(CorpSuiteAuthConverter.CorpSuiteAuthDO2CorpSuiteAuthVO(item));
            }
            return ServiceResult.success(resultList);
        } catch (Exception e) {
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }


    @Override
    public ServiceResult<CorpSuiteCallBackVO> getCorpCallback(String suiteKey, String corpId) {
        try{
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ));
            ServiceResult<CorpTokenVO> tokenSr = corpManageService.getCorpToken(suiteKey, corpId);
            ServiceResult<CorpSuiteCallBackVO> sr = crmOapiRequestHelper.getCorpSuiteCallback(suiteKey, corpId, tokenSr.getResult().getCorpToken());
            if(sr.isSuccess()){
                return sr;
            }
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }catch (Exception e){
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }


    @Override
    public ServiceResult<Void> saveCorpCallback(String suiteKey, String corpId,String callBakUrl, List<String> tagList) {
        try{
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("callBakUrl", callBakUrl),
                    LogFormatter.KeyValue.getNew("tagList", JSON.toJSONString(tagList))
            ));
            ServiceResult<SuiteVO> suiteSr = suiteManageService.getSuiteByKey(suiteKey);
            ServiceResult<CorpTokenVO> tokenSr = corpManageService.getCorpToken(suiteKey, corpId);
            String corpToken = tokenSr.getResult().getCorpToken();
            ServiceResult<CorpSuiteCallBackVO> callBackSr = crmOapiRequestHelper.getCorpSuiteCallback(suiteKey, corpId, corpToken);
            ServiceResult<Void> sr = null;
            if(!callBackSr.isSuccess()||null==callBackSr.getResult()){
                sr = crmOapiRequestHelper.saveCorpCallback(suiteKey, corpId, tokenSr.getResult().getCorpToken(), suiteSr.getResult().getToken(), suiteSr.getResult().getEncodingAesKey(), callBakUrl, tagList);
            }else{
                sr = crmOapiRequestHelper.updateCorpSuiteCallback(suiteKey, corpId, tokenSr.getResult().getCorpToken(), suiteSr.getResult().getToken(), suiteSr.getResult().getEncodingAesKey(), callBakUrl, tagList);
            }
            if(!sr.isSuccess()){
                return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
            }
            return ServiceResult.success(null);
        }catch (Exception e){
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId)
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }


    @Override
    public ServiceResult<Void> updateCorpCallback(String suiteKey, String corpId,String callBakUrl, List<String> tagList) {
        try{
            bizLogger.info(LogFormatter.getKVLogData(LogFormatter.LogEvent.START,
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("callBakUrl", callBakUrl),
                    LogFormatter.KeyValue.getNew("tagList", JSON.toJSONString(tagList))
            ));
            ServiceResult<SuiteVO> suiteSr = suiteManageService.getSuiteByKey(suiteKey);
            ServiceResult<CorpTokenVO> tokenSr = corpManageService.getCorpToken(suiteKey, corpId);
            ServiceResult<Void> sr = crmOapiRequestHelper.updateCorpSuiteCallback(suiteKey, corpId, tokenSr.getResult().getCorpToken(), suiteSr.getResult().getToken(), suiteSr.getResult().getEncodingAesKey(), callBakUrl, tagList);
            if(sr.isSuccess()){
                return sr;
            }
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }catch (Exception e){
            bizLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("callBakUrl", callBakUrl),
                    LogFormatter.KeyValue.getNew("tagList", JSON.toJSONString(tagList))
            ), e);
            mainLogger.error(LogFormatter.getKVLogData(LogFormatter.LogEvent.END,
                    "系统异常",
                    LogFormatter.KeyValue.getNew("suiteKey", suiteKey),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("corpId", corpId),
                    LogFormatter.KeyValue.getNew("callBakUrl", callBakUrl),
                    LogFormatter.KeyValue.getNew("tagList", JSON.toJSONString(tagList))
            ), e);
            return ServiceResult.failure(ServiceResultCode.SYS_ERROR.getErrCode(),ServiceResultCode.SYS_ERROR.getErrMsg());
        }
    }



}
