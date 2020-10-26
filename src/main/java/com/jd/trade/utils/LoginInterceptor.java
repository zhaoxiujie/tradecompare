package com.jd.trade.utils;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lienpeng
 * @date: 2019/10/9 10:14
 */

public class LoginInterceptor implements HandlerInterceptor {


    private List<String> url = new ArrayList<>();


    /**
     * 开始进入地址请求拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Cookie[] cookies = request.getCookies();
        String ticket = request.getParameter("sso_service_ticket");
        //校验ticket有效性
        if (ticket != null && checkTicket(ticket, request, response)){
            return true;
        }
        if(cookies != null){
            for(Cookie cookie : cookies){
                if("sso.jd.com".equals(cookie.getName())){
                    if (checkCookie(cookie.getValue(), request, response)){
                        return true;
                    }
                    break;
                }
            }
        }
        response.sendRedirect("http://ssa.jd.com/sso/login?ReturnUrl="+ request.getRequestURL());
        return false;
    }

    /**
     * 处理请求完成后视图渲染之前的处理操作
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

    }

    /**
     * 视图渲染之后的操作
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {

    }

    /**
     * 定义排除拦截URL
     * @return
     */
    public List<String> getUrl(){
        url.add("/error");
        return url;
    }

    /**
     * 定义拦截URL
     * @return
     */
    public List<String> addUrl(){
        List<String> urlList = new ArrayList<>();
        urlList.add("/home/home");
        return urlList;
    }
    /**
     * 校验ticket有效性
     */
    private boolean checkTicket(String ticket, HttpServletRequest request, HttpServletResponse response){
        String checkTicketUrl = "http://ssa.jd.com/sso/ticket/getTicket?sso_service_ticket=";
        String param = "&url=" + request.getRequestURL()  + "&ip=" + request.getLocalAddr();
        String tempUrl = checkTicketUrl + ticket + param;
        HttpUtils httpUtils = new HttpUtils();
        String result = httpUtils.doGet(tempUrl,"");
        JSONObject jsonObject = JSONObject.parseObject(result);
        if ((Boolean) jsonObject.get("REQ_FLAG")){
            Cookie cookie = new Cookie("sso.jd.com",String.valueOf(jsonObject.get("REQ_DATA")));
            response.addCookie(cookie);
            return true;
        }
        return false;
    }

    /**
     * 校验cookie有效性
     */
    private boolean checkCookie(String ssoCookie, HttpServletRequest request, HttpServletResponse response){
        String verifyTicketUrl = "http://ssa.jd.com/sso/ticket/verifyTicket?ticket=";
        String param = "&url=" + request.getRequestURL()  + "&ip=" + request.getLocalAddr();
        String tempUrl = verifyTicketUrl + ssoCookie + param;
        HttpUtils httpUtils = new HttpUtils();
        String result = httpUtils.doGet(tempUrl,"");
        JSONObject jsonObject = JSONObject.parseObject(result);
        return (Boolean) jsonObject.get("REQ_FLAG");
    }
}
