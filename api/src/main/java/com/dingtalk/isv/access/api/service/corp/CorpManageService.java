package com.dingtalk.isv.access.api.service.corp;


import com.dingtalk.isv.access.api.model.corp.CorpAppVO;
import com.dingtalk.isv.access.api.model.corp.CorpJSAPITicketVO;
import com.dingtalk.isv.access.api.model.corp.CorpTokenVO;
import com.dingtalk.isv.access.api.model.corp.CorpVO;
import com.dingtalk.isv.common.model.ServiceResult;

import java.util.List;
/**
 * 企业增删改查管理功能
 * 企业访问开放平台Token管理功能
 * Created by 浩倡 on 16-1-17.
 */
public interface CorpManageService {
    /**
     * 增加或者修改一个企业信息
     * @param corpVO
     * @return
     */
    public ServiceResult<Void> saveOrUpdateCorp(CorpVO corpVO);

    /**
     * 获取企业访问开放平台token
     * @param suiteKey
     * @param corpId
     * @return
     */
    public ServiceResult<CorpTokenVO> getCorpToken(String suiteKey, String corpId);

    /**
     * 删除企业token
     * @param suiteKey
     * @param corpId
     * @return
     */
    public ServiceResult<CorpTokenVO> deleteCorpToken(String suiteKey,String corpId);

    /**
     * 获取企业访问开放平台js ticket
     * @return
     */
    public ServiceResult<CorpJSAPITicketVO> getCorpJSAPITicket(String suiteKey, String corpId);

    /**
     * 获取企业开通的套件下的微应用
     * @param corpId
     * @return
     */
    public ServiceResult<CorpAppVO>  getCorpApp(String corpId, Long appId);




}
