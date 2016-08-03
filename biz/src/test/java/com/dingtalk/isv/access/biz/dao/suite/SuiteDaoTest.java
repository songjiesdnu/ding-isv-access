package com.dingtalk.isv.access.biz.dao.suite;

import com.alibaba.fastjson.JSON;
import com.dingtalk.isv.access.api.model.suite.SuiteVO;
import com.dingtalk.isv.access.biz.suite.model.SuiteDO;
import com.dingtalk.isv.access.biz.suite.model.helper.SuiteConverter;
import com.dingtalk.isv.access.biz.base.BaseTestCase;
import com.dingtalk.isv.access.biz.suite.dao.SuiteDao;
import org.junit.Test;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;

/**
 */
public class SuiteDaoTest extends BaseTestCase {

    @Resource
    private SuiteDao suiteDao;

    @Test
    public void test_insert() {
        String suiteName = "浩倡接入套件日常";
        String suiteKey = "suiteytzpzchcpug3xpsm";
        String suiteSecret = "mSk7Q5CpjoIgkjyb015E43qUkTkQyAUtsnRl0eK7Jr1_t19LecVThDSVeP2VbaTy";
        String encodingAesKey= "yKO0z538zCNFxcFuuSzJmtPQCp5Me6MOzuBoK7KgoIc";
        String token = "122348";
        String eventReceiveUrl = "";
        SuiteVO suiteVO = new SuiteVO();
        suiteVO.setSuiteName(suiteName);
        suiteVO.setSuiteKey(suiteKey);
        suiteVO.setSuiteSecret(suiteSecret);
        suiteVO.setToken(token);
        suiteVO.setEncodingAesKey(encodingAesKey);
        suiteVO.setEventReceiveUrl(eventReceiveUrl);
        SuiteDO suiteDO = SuiteConverter.SuiteVO2SuiteDO(suiteVO);
        suiteDao.addSuite(suiteDO);
    }

    @Test
    public void test_getSuiteByKey() {
        SuiteDO suiteDO = suiteDao.getSuiteByKey("suite6ni7vdaqzpwlvger");
        System.out.println(suiteDO);
        Assert.isTrue(null!=suiteDO);
    }



    @Test
    public void test_getAllSuiteByKey() {
        List<SuiteDO> list = suiteDao.getAllSuite();
        System.out.println(JSON.toJSON(list));
        Assert.isTrue(null!=list);
    }
}
