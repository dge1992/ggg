package com.github.dge1992.commonforward.template;

import com.alibaba.fastjson.JSON;
import com.github.dge1992.commonforward.api.enums.HttpMethodEnum;
import com.github.dge1992.commonforward.api.model.BaseResult;
import com.github.dge1992.commonforward.api.model.CommonReceiveObject;
import com.github.dge1992.commonforward.api.model.HttpForwardResult;
import com.github.dge1992.commonforward.biz.common.BeanNameService;
import com.github.dge1992.commonforward.biz.post.PostStrategyHandler;
import com.github.dge1992.commonforward.biz.post.strategy.PostStrategy;
import com.github.dge1992.commonforward.biz.pre.PreStrategyHandler;
import com.github.dge1992.commonforward.biz.pre.strategy.PreStrategy;
import com.github.dge1992.commonforward.common.utils.CommonHandler;
import com.github.dge1992.commonforward.common.utils.SpringContextHolder;
import com.github.dge1992.commonforward.lib.middleware.httpclient.HttpClientHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Optional;

/**
 * @author dge
 * @version 1.0
 * @date 2021-01-19 11:12
 */
@Component
public class HttpForwardTemplate extends BaseForwardTemplate{

    @Autowired
    private BeanNameService beanNameService;

    @Autowired
    private HttpClientHandler httpClientHandler;

    @Autowired
    private CommonHandler commonHandler;

    @Override
    protected void preExecute(CommonReceiveObject receiveObject) {
        //前置处理
        String preStrategyCode = receiveObject.getPreStrategyCode();
        beanNameService.getBeanName(preStrategyCode).ifPresent(e -> {
            PreStrategy preStrategy = SpringContextHolder.getBean(e);
            PreStrategyHandler preStrategyHandler = new PreStrategyHandler(preStrategy);
            preStrategyHandler.execute(receiveObject);
        });
    }

    @Override
    protected BaseResult send(CommonReceiveObject receiveObject) {
        //发送
        HttpForwardResult httpResult = null;
        try {
            //请求参数
            HashMap<String, Object> paramsMap = receiveObject.getParamsMap();
            //请求方式
            Integer methodCode = receiveObject.getMethod();
            HttpMethodEnum method = HttpMethodEnum.getByMethodCode(methodCode);
            //请求路径 传过来的url优先，没有则根据code查询
            Optional<String> optionalUrl = commonHandler.getUrl(receiveObject);
            String url = optionalUrl.orElseThrow(NullPointerException::new);
            //头信息
            HashMap<String, String> headMap = receiveObject.getHeadMap();
            if(null == method){
                throw new NullPointerException("method is null");
            }
            switch (method){
                case GET:
                    //get请求
                    httpResult = httpClientHandler.doGet(url, paramsMap);
                    break;
                case POST_JSON:
                    //post json形式请求
                    httpResult = httpClientHandler.doPostJson(url, JSON.toJSONString(paramsMap), headMap);
                    break;
                case POST_PARAM:
                    //post param形式请求
                    httpResult = httpClientHandler.doPost(url, paramsMap, headMap);
                    break;
                default:
                    break;
            }
            //设置返回结果
            receiveObject.setResult(JSON.toJSONString(httpResult));
        } catch (Exception e){
            e.printStackTrace();
        }
        return httpResult;
    }

    @Override
    protected void postExecute(BaseResult result, CommonReceiveObject receiveObject) {
        //后置处理
        String postStrategyCode = receiveObject.getPostStrategyCode();
        String resultStr = receiveObject.getResult();
        beanNameService.getBeanName(postStrategyCode).ifPresent(e -> {
            PostStrategy postStrategy = SpringContextHolder.getBean(e);
            PostStrategyHandler postStrategyHandler = new PostStrategyHandler(postStrategy);
            postStrategyHandler.execute(receiveObject, resultStr);
        });
    }
}
