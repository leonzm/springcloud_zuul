package com.example.springcloud_zuul.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author: Leon
 * @CreateDate: 2017/7/27
 * @Description: Access token 过滤器
 * @Version: 1.0.0
 */
public class AccessFilter extends ZuulFilter {

    private static Logger LOGGER = LoggerFactory.getLogger(AccessFilter.class);

    /**
     * 过滤器类型
     * @return
     */
    @Override
    public String filterType() {
        return "pre";
    }

    /**
     * 过滤器的执行顺序
     * @return
     */
    @Override
    public int filterOrder() {
        return 0;
    }

    /**
     * 判断该过滤器是否需要被执行
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * 过滤器的具体逻辑
     * @return
     */
    @Override
    public Object run() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();

        LOGGER.info("send {} request to {}", request.getMethod(), request.getRequestURL().toString());

        Object accessToken = request.getParameter("accessToken");
        if (accessToken == null) {
            LOGGER.warn("access token is empty");
            requestContext.setSendZuulResponse(false); // 令 zuul 过滤该请求，不对其进行路由
            requestContext.setResponseStatusCode(401); // 设置返回错误码
            requestContext.setResponseBody("access token is empty");
        }
        LOGGER.info("access token ok");
        return null;
    }

}
