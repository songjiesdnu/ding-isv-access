package com.dingtalk.isv.access.web.test;

import com.dingtalk.isv.access.api.model.corp.CorpTokenVO;
import com.dingtalk.isv.common.model.ServiceResult;

/**
 * Created by zhangjin.jsf@taobao.com at 2016-01-11 18:32
 */
public interface TestService {

    String hello(String name);


    ServiceResult<CorpTokenVO> hello1(String name, String name2);

    ServiceResult<Integer> add(int a,int b);
}
