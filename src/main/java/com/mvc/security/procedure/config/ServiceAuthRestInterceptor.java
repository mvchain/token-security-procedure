package com.mvc.security.procedure.config;

import com.github.pagehelper.PageHelper;
import com.mvc.common.context.BaseContextHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author qyc
 */
public class ServiceAuthRestInterceptor extends HandlerInterceptorAdapter {

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContextHandler.remove();
        super.afterCompletion(request, response, handler, ex);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        setPage(request);
        return super.preHandle(request, response, handler);
    }

    public void setPage(HttpServletRequest request) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String pageNo = request.getParameter("pageNum");
            String pageSize = request.getParameter("pageSize");
            String orderBy = request.getParameter("orderBy");
            if (StringUtils.isNotBlank(pageNo) && StringUtils.isNotBlank(pageSize)) {
                PageHelper.startPage(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
            }
            if (StringUtils.isNotBlank(orderBy)) {
                PageHelper.orderBy(orderBy);
            }
        }
    }

}
