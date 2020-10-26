package com.jd.trade.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.jd.order.purchase.config.client.xmlext.service.XmlConfigService;
import com.jd.order.purchase.config.client.xmlext.util.FullXmlSerializableTool;
import com.jd.purchase.domain.old.bean.*;
import com.jd.trade.beans.OrderXmlIgoreConfigs;
import com.jd.trade.utils.FileUtils;
import com.jd.trade.utils.HttpUtils;
import net.sf.json.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Lienpeng
 * @date: 2019/9/25 10:31
 */
@Service
public class OrderService {

    @Value("${properties.ignore}")
    private String ignoreProperties;
    @Resource
    FileUtils fileUtils;
    /**
     * DBConfig配置
     */
    @Autowired
    private XmlConfigService configService;

    private String orderURL = "http://orderver.jd.care/orderXml.xml?orderId=%s&ver=0";
    private String cartURL = "http://orderver.jd.care/cartXml.xml?orderId=%s&ver=0";

    /**
     * 根据订单号从orderver获取订单信息
     * @param orderId
     * @param request
     * @param flag 1获取orderXML 2获取cartXML
     * @return
     */
    public String getContent(String orderId, HttpServletRequest request, int flag){
        String ssoCookie = "";
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies){
            if ("sso.jd.com".equals(cookie.getName())){
                ssoCookie = "sso.jd.com=" + cookie.getValue();
                break;
            }
        }
        String url;
        if (flag == 1){
            url = String.format(orderURL,orderId);
        }else {
            url = String.format(cartURL,orderId);
        }

        HttpUtils httpUtils = new HttpUtils();
        return httpUtils.doGet(url,ssoCookie);
    }

    public Map<String,String[]> getDiff(String onlineXml, String compXml){
        Order compOrder = FullXmlSerializableTool.deSerializeXML(Order.class, compXml, true);
        Order onlineOrder = FullXmlSerializableTool.deSerializeXML(Order.class, onlineXml, true);
        //排序
        orderSort(compOrder);
        orderSort(onlineOrder);
        //获取所有不同的节点
        Map<String,String[]> diffMap = compareOrder(onlineOrder, compOrder);
        //剔除无关元素
        blankProperty(diffMap);
        //特殊处理
        specialHandler(diffMap);
        return diffMap;
    }

    /***
     * 调整对象中的字段顺序
     * @param order
     */
    private void orderSort(Order order){

        //tags排序
        List<Integer> newTags = order.getTags();
        Collections.sort(newTags);

        //orderShipmentInfo sku排序
        List<OrderShipmentInfo> shipmentInfoList = order.getShipmentInfoList();
        for (OrderShipmentInfo orderShipmentInfo : shipmentInfoList){
            if (orderShipmentInfo.getSupportedSkuIdList()!=null){
                Collections.sort(orderShipmentInfo.getSupportedSkuIdList());
            }
            if (orderShipmentInfo.getSupportedSkuUuidList()!=null){
                Collections.sort(orderShipmentInfo.getSupportedSkuUuidList());
            }

        }

        //OrderPaymentInfo sku排序
        List<OrderPaymentInfo> paymentInfoList = order.getPaymentInfoList();
        for (OrderPaymentInfo orderPaymentInfo : paymentInfoList) {
            if (orderPaymentInfo.getSupportedSkuIdList()!=null){
                Collections.sort(orderPaymentInfo.getSupportedSkuIdList());
            }
            if (orderPaymentInfo.getSupportedSkuUuidList()!=null){
                Collections.sort(orderPaymentInfo.getSupportedSkuUuidList());
            }

        }

        //Freight sku排序
        List<FreightEntity> freightInfo = order.getFreightInfo();
        for (FreightEntity freightEntity : freightInfo){
            List<Freight> freightList = freightEntity.getFreightList();
            for (Freight freight : freightList){
                List<String> skuUuidList = freight.getSkuUuidList();
                if (skuUuidList != null){
                    Collections.sort(skuUuidList);
                }
                SkuIdList skuIdList = freight.getSkuIdList();
                if (skuIdList != null){
                    Collections.sort(skuIdList.getSkuIdList());
                }
            }
        }
    }
    private Map<String,String[]> compareOrder(Order onlineOrder, Order compOrder){
        Map<String, String> onlineExtTags = onlineOrder.getExtTags();
        Map<String, String> compExtTags = compOrder.getExtTags();
        Map<String,String[]> contents = new HashMap<>();
        //轮询线上ExtTags key
        for (Map.Entry<String, String> onlineExtTagEntry : onlineExtTags.entrySet()) {
            String key = onlineExtTagEntry.getKey();
            String value = onlineExtTagEntry.getValue();
            //缺失节点
            if (!compExtTags.containsKey(key)) {
                String[] result = {value,"节点丢失"};
                contents.put("^TheExtTags/"+key,result);
            } else {
                //比较节点值
                String info = "";
                String compValue = compExtTags.get(key);
                if (value == null){
                    if (compValue != null){
                        String[] result = {"空值",compValue};
                        contents.put("^TheExtTags/"+key,result);
                    }
                }else {
                    if (compValue == null){
                        String[] result = {value,"空值"};
                        contents.put("^TheExtTags/"+key,result);
                    }else if (!value.equals(compValue)){
                        String[] result = {value,compValue};
                        contents.put("TheExtTags/"+key,result);
                    }
                }
            }
        }
        for (Map.Entry<String, String> compExtTagEntry : compExtTags.entrySet()){
            String key = compExtTagEntry.getKey();
            String value = compExtTagEntry.getValue();
            if (!onlineExtTags.containsKey(key)){
                String[] result = {"未包含节点",value};
                contents.put("^TheExtTags/"+key,result);
            }
        }
        Map<String,String> map = new HashMap<>();
        map.put("same","same");
        onlineOrder.setExtTags(map);
        compOrder.setExtTags(map);
        String onlineXML = FullXmlSerializableTool.serializeXML(onlineOrder, true);
        String compXML = FullXmlSerializableTool.serializeXML(compOrder, true);
        contents.putAll(fileUtils.compareXML(onlineXML,compXML));

        return contents;
    }




    //筛选无关项
    private void blankProperty(Map<String,String[]> map){

        List<String> ignoreList = new ArrayList<>(Arrays.asList(ignoreProperties.split(" ")));
        OrderXmlIgoreConfigs orderXmlIgoreConfigs = configService.getConfigByTypeId(20196601, OrderXmlIgoreConfigs.class);
        List<String> orderXmlIgoreList = orderXmlIgoreConfigs.getOrderXmlIgoreList();
        for (String property : orderXmlIgoreList){
            map.remove(property);
        }
    }

    //特殊处理
    private void specialHandler(Map<String,String[]> diffMap){

        String promiseTime = "TheExtTags/promiseTime";
        if (diffMap.containsKey(promiseTime)){
            String onlineJson = diffMap.get(promiseTime)[0];
            String compJson = diffMap.get(promiseTime)[1];
            Map<String,Map<String,String>> onlineMap = (Map) JSON.parse(onlineJson);
            Map<String,Map<String,String>> compMap = (Map) JSON.parse(compJson);
            if (onlineMap.equals(compMap)){
                diffMap.remove(promiseTime);
            }else {
                String sortedonlineMap = new TreeMap<String, Map<String, String>>(onlineMap).toString();
                String sortedcompMap = new TreeMap<String, Map<String,String>>(compMap).toString();
                String[] promiseTimes = {sortedonlineMap,sortedcompMap};
                diffMap.put(promiseTime,promiseTimes);
            }
        }


        String shopIdFlags = "TheExtTags/shopIdFlags";
        if (diffMap.containsKey(shopIdFlags)){
            Comparator comparator = (Comparator<Map<String, String>>) (o1, o2) -> {
               if (!o1.get("shopId").equals(o2.get("shopId"))){
                   return o1.get("shopId").compareTo(o2.get("shopId"));
               }else {
                   return String.valueOf(o1.get("skuId")).compareTo(String.valueOf(o2.get("skuId")));
               }
            };
            String onlineJson = diffMap.get(shopIdFlags)[0];
            String compJson = diffMap.get(shopIdFlags)[1];
            List<Map<String,String>> onlineList = (List<Map<String,String>>) JSON.parse(onlineJson);
            List<Map<String,String>> compList = (List<Map<String,String>>) JSON.parse(compJson);
            Collections.sort(onlineList, comparator);
            Collections.sort(compList, comparator);
            if (onlineList.size() == compList.size() && onlineList.equals(compList)){
                    diffMap.remove(shopIdFlags);
            }else{
                diffMap.put(shopIdFlags,new String[]{onlineList.toString(),compList.toString()});
            }
        }

        String sendPayDict = "TheExtTags/SendPayDict";
        if (diffMap.containsKey(sendPayDict)) {
            List<String> onlineJson = new ArrayList<>(Arrays.asList(diffMap.get(sendPayDict)[0].split("\\|")));
            List<String> compJson = new ArrayList<>(Arrays.asList(diffMap.get(sendPayDict)[1].split("\\|")));

            Iterator<String> iterator = onlineJson.iterator();
            while (iterator.hasNext()) {
                String item = iterator.next();
                if (compJson.contains(item)) {
                    iterator.remove();
                    compJson.remove(item);
                }
            }

            if (onlineJson.size()==0 && compJson.size()==0){
                diffMap.remove(sendPayDict);
            }else{
                String onlineString = String.join(",", onlineJson);
                String compString = String.join(",", compJson);
                String[] result = {onlineString,compString};
                diffMap.remove(sendPayDict);
                diffMap.put(sendPayDict+"差异点",result);
            }
        }
        // 对比优惠券支持skuId的扩展节点
        String couponSupportSkuIDs = "TheExtTags/couponSupportSkuIDs";
        if (diffMap.containsKey(couponSupportSkuIDs)) {

            String onlineJson = diffMap.get(couponSupportSkuIDs)[0];
            String compJson = diffMap.get(couponSupportSkuIDs)[1];
            Map<String, List<Long>> onlineMap = (Map<String, List<Long>>) JSON.parse(onlineJson);
            Map<String, List<Long>> compMap = (Map<String, List<Long>>) JSON.parse(compJson);
            if (MapUtils.isNotEmpty(onlineMap) && MapUtils.isNotEmpty(compMap)) {
                Set<String> sameKey = new HashSet<>();
                for (Map.Entry<String, List<Long>> stringListEntry : onlineMap.entrySet()) {
                    String onlineKey = stringListEntry.getKey();

                    if (CollectionUtils.isNotEmpty(stringListEntry.getValue()) && CollectionUtils.isNotEmpty(compMap.get(onlineKey))) {
                        // 线上
                        List<Long> onlineValue = new ArrayList<>();
                        Object[] objects = stringListEntry.getValue().toArray();
                        for (Object aLong : objects) {
                            if (aLong instanceof Integer) {
                                onlineValue.add(((Integer) aLong).longValue());
                            } else {
                                onlineValue.add((long)aLong);
                            }

                        }
                        // 组件化
                        List<Long> compValue = new ArrayList<>();
                        Object[] objects1 = compMap.get(onlineKey).toArray();
                        for (Object compLong : objects1) {
                            if (compLong instanceof Integer) {
                                compValue.add(((Integer) compLong).longValue());
                            } else {
                                compValue.add((long)compLong);
                            }
                        }
                        Collections.sort(onlineValue);
                        Collections.sort(compValue);
                        if (onlineValue.equals(compValue)) {
                            sameKey.add(stringListEntry.getKey());
                        }
                    }
                }
                // 如果相同Key的个数和本身要对比的个数相同，说明都一致
                if (sameKey.size() == onlineMap.entrySet().size()) {
                    diffMap.remove(couponSupportSkuIDs);
                }
            }
            
        }
    }
}
