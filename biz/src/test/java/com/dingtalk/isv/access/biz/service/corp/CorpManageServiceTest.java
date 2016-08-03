package com.dingtalk.isv.access.biz.service.corp;

import com.alibaba.fastjson.JSON;
import com.dingtalk.isv.access.api.model.corp.CorpJSAPITicketVO;
import com.dingtalk.isv.access.api.service.corp.CorpManageService;
import com.dingtalk.isv.access.biz.base.BaseTestCase;
import com.dingtalk.isv.common.model.ServiceResult;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * Created by mint on 16-1-22.
 */
public class CorpManageServiceTest extends BaseTestCase {


    @Resource
    private CorpManageService corpManageService;
    @Test
    public void test_getCorpToken() {
        String corpId="ding4ed6d279061db5e7";
        String suiteKey="suiteytzpzchcpug3xpsm";
        corpManageService.getCorpToken(suiteKey,corpId);

        //ServiceResult<List<SuiteVO>> sr = corpManageService.getCorpToken()
        //System.out.println(JSON.toJSON(sr));
        //Assert.isTrue(null!=sr.getResult());
    }

    @Test
    public void test_getCorpJSAPITicket() {
        Long startTime =System.currentTimeMillis();
        String corpId="ding4ed6d279061db5e7";
        String suiteKey="suiteytzpzchcpug3xpsm";
        ServiceResult<CorpJSAPITicketVO> sr = corpManageService.getCorpJSAPITicket(suiteKey, corpId);
        System.err.println(JSON.toJSONString(sr));
        System.err.println("rt:"+(System.currentTimeMillis()-startTime));
    }


    @Test
    public void test_getCorpJSAPITicket11() {
//        Long startTime =System.currentTimeMillis();
//        String corpId="ding4ed6d279061db5e7";
//        String suiteKey="suiteytzpzchcpug3xpsm";
//        ServiceResult<CorpJSAPITicketVO> sr = corpManageService.getCorpAppList()
//        System.err.println(JSON.toJSONString(sr));
//        System.err.println("rt:"+(System.currentTimeMillis()-startTime));
    }




}
