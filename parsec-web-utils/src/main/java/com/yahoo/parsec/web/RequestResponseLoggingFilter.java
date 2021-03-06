// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.

package com.yahoo.parsec.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RequestResponseLoggingFilter implements Filter {

    private ServletRequestResponseFomatter formatter;
    private List<String> logMethodList = Arrays.asList(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE);
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String formatterClassName = filterConfig.getInitParameter("formatter-classname");
        if (formatterClassName == null) {
            throw new ServletException("Init param formatter-classname is required");
        }
        try {
            formatter = (ServletRequestResponseFomatter) Class.forName(formatterClassName).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new ServletException("Cannot create formatter instance", e);
        }
        String logMethods = filterConfig.getInitParameter("log-methods");
        if (logMethods != null) {
            logMethodList = Arrays.asList(logMethods.split("\\s*\\|\\s*"));
            logMethodList.replaceAll(String::toUpperCase);
        }
    }

    private boolean logConditionMetForReqAndResp(HttpServletRequest request) {
        return logMethodList.contains(request.getMethod());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("RequestResponseLoggingFilter just supports HTTP requests");
        }
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        ParsecServletRequestWrapper requestWrapper = new ParsecServletRequestWrapper(httpReq);
        ParsecServletResponseWrapper responseWrapper = new ParsecServletResponseWrapper(httpResp);

        chain.doFilter(requestWrapper, responseWrapper);

        if (logConditionMetForReqAndResp(requestWrapper)) {
            String httpTransaction = formatter.format(requestWrapper, responseWrapper, null);
            LOGGER.debug(httpTransaction);
        }
    }

    @Override
    public void destroy() {

    }
}
