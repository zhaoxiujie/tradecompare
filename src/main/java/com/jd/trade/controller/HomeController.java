package com.jd.trade.controller;

import com.jd.common.util.StringUtils;
import com.jd.fastjson.JSONObject;
import com.jd.trade.service.CartService;
import com.jd.trade.service.OrderService;
import com.jd.trade.service.sendPay;
import com.jd.trade.utils.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lienpeng
 * @date: 2019/8/8 16:03
 */
@Controller
@RequestMapping("/")
public class HomeController {
    @Resource
    OrderService orderSerice;
    @Resource
    CartService cartService;
    @Resource
    FileUtils fileUtils;
    @Resource
    sendPay sendpays;


    /**
     * 原始比对订单接口
     * @param onlineId
     * @param compId
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    @RequestMapping("com/{online}/{comp}")
    @ResponseBody
    public String getAndCompare(@PathVariable String onlineId, @PathVariable String compId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String ticket = request.getParameter("sso_service_ticket");
        if (ticket!=null && ticket.length()>0){
            response.sendRedirect(request.getRequestURL().toString());
            return "";
        }
        String online = orderSerice.getContent(onlineId,request,1);
        String compare = orderSerice.getContent(compId,request,1);
        Map<String,String[]> diffMap = orderSerice.getDiff(online,compare);
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String[]> entry : diffMap.entrySet()){
            String temp = String.format("【%s】\n线上的值为：%s，\n组件化值为：%s。",entry.getKey(),entry.getValue()[0],entry.getValue()[1]);
            String htmlString = temp.replace("\n","<br >");
            result.append(htmlString).append("<br><hr>");
        }
        return result.toString();
    }


    /**
     * 请求转移
     * @param model
     * @param aaa
     * @return
     */
    @GetMapping(value = "/home/{aaa}")
    public String home(Model model, @PathVariable("aaa") String aaa) {
        if (StringUtils.isBlank(aaa)) {
            return "home";
        } else {
            return aaa;
        }
    }

    /**
     * 获取订单信息并返回比较结果
     * @param params
     * @param request
     * @return
     */
    @PostMapping(value = "/home/getAndCompare")
    @ResponseBody
    public Map compare(@RequestBody JSONObject params, HttpServletRequest request){
        //获取订单信息
        String onlineOrder = orderSerice.getContent((String) params.get("online"),request,1);
        String compareOrder = orderSerice.getContent((String) params.get("compare"),request,1);
        String onlineCart = orderSerice.getContent((String) params.get("online"),request,2);
        String compareCart = orderSerice.getContent((String) params.get("compare"),request,2);
        Map<String, String[]> diffCart = cartService.compare(onlineCart, compareCart);
        //比较orderXML
        Map<String,String[]> diffOrder = orderSerice.getDiff(onlineOrder,compareOrder);

        Map<String,String> map = new HashMap<>();
        map.put("online_order",fileUtils.stringToXML(onlineOrder));
        map.put("compare_order",fileUtils.stringToXML(compareOrder));
        map.put("online_cart",fileUtils.stringToXML(cartService.removeExtTagPair(onlineCart)));
        map.put("compare_cart",fileUtils.stringToXML(cartService.removeExtTagPair(compareCart)));
        map.put("resultOrder",fileUtils.mapToString(diffOrder));
        map.put("resultCart",fileUtils.mapToString(diffCart));
        return map;
    }

    /***
     * 通过文本进行比较
     * @param params
     * @return
     */
    @PostMapping(value = "/home/compareOrder")
    @ResponseBody
    public String compareOrder(@RequestBody JSONObject params){
        String online = (String) params.get("online");
        String compare = (String) params.get("compare");
        //比较orderXML
        Map<String,String[]> diffMap = orderSerice.getDiff(online,compare);
        return fileUtils.mapToString(diffMap);
    }
    @PostMapping(value = "/home/compareCart")
    @ResponseBody
    public String compareCart(@RequestBody JSONObject params){
        String onlineCart = (String) params.get("onlineCart");
        String compareCart = (String) params.get("compareCart");
        Map<String,String[]> diffMapCart = cartService.compare(onlineCart,compareCart);
        return fileUtils.mapToString(diffMapCart);
    }

    @PostMapping(value = "/home/getsendpay")
    @ResponseBody
    public String sendpay(@RequestBody JSONObject param1){
        return sendpays.setSendPay((String)param1.get("sendpay"),Integer.valueOf((String)param1.get("num")));
    }
}
