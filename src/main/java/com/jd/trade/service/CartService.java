package com.jd.trade.service;

import com.jd.fastjson.JSON;
import com.jd.order.purchase.config.client.xmlext.util.FullXmlSerializableTool;
import com.jd.purchase.domain.old.bean.*;
import com.jd.trade.utils.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Lienpeng
 * @date: 2019/12/10 11:17
 */
@Service
public class CartService {
    @Resource
    FileUtils fileUtils;
    public Map<String,String[]> compare(String online, String compare){
        Map<String,Map> onlineExtTag = cartXMLExtTagPairCollect(online);
        Map<String,Map> compareExtTag = cartXMLExtTagPairCollect(compare);
        Map<String, String[]> resultMap = compareExtTagPair(onlineExtTag, compareExtTag);
        online = sortSku(online);
        compare = sortSku(compare);
        String onlineStr = removeExtTagPair(online);
        String compareStr = removeExtTagPair(compare);
        resultMap.putAll(fileUtils.compareXML(onlineStr,compareStr));
        return resultMap;
    }

    /**
     * ThePacks节点排序
     */
    public String sortSku(String cartXML){
        Cart cart = FullXmlSerializableTool.deSerializeXML(Cart.class, cartXML, true);
        Suit[] suits = cart.getThePacks();
        if (suits != null){
            Arrays.sort(suits, (suit1, suit2) -> suit1.getId()>suit2.getId() ? 1 : -1);
            for (Suit suit : suits){
                List<SKU> skus = suit.getTheSkus();
                if (skus != null){
                    skus.sort((sku1, sku2) -> sku1.getId() > sku2.getId() ? 1 : -1);
                }
            }
            return FullXmlSerializableTool.serializeXML(cart, true);
        }

        return cartXML;
    }

    /**
     * 去掉所有的ExtTagPair节点
     * @param string
     * @return
     */
    public String removeExtTagPair(String string){
        Document document = null;
        Map<String,Map> result = new HashMap<>();
        try {
            document = DocumentHelper.parseText(string);
            Element root = document.getRootElement();
            List<Element> list = document.selectNodes("//TheExtTags");
            for (Element theExtTagsElement : list) {
                theExtTagsElement.getParent().remove(theExtTagsElement);
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return document.asXML();

    }
    /**
     *遍历全部的ExtTagPair节点
     * @param string
     * @return
     */
    public Map<String,Map> cartXMLExtTagPairCollect(String string) {
        Document document = null;
        Map<String,Map> result = new HashMap<>();
        try {
            document = DocumentHelper.parseText(string);
            Element root = document.getRootElement();
            List<Element> list = document.selectNodes("//TheExtTags");
            for (Element theExtTagsElement : list) {
                Element skuElement = theExtTagsElement.getParent();
                String skuId = skuElement.element("Id").getTextTrim();
                String skuPath = skuElement.getPath();
                String sku = skuPath+"/"+skuId+"/TheExtTags/";
                Map<String,String> extTagPairs = new HashMap<>();
                for (Element extTagPairElement : (List<Element>)theExtTagsElement.elements("ExtTagPair")){
                    extTagPairs.put(extTagPairElement.elementText("Key"),extTagPairElement.elementText("Val"));
                }
                result.put(sku,extTagPairs);
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return result;
    }
    /**
     * 比较全部的ExtTagPair
     */
    public Map<String,String[]> compareExtTagPair(Map<String,Map> online, Map<String,Map> compare){
        Map<String,String[]> resultMap = new HashMap<>();
        Set<String> set = online.keySet();
        for (String sku: set) {
            if (compare.containsKey(sku)) {
                Map<String,String> onlineExtTagPair = online.get(sku);
                Map<String,String> compareExtTagPair = compare.get(sku);
                for (Map.Entry<String, String> onlineExtTagEntry : onlineExtTagPair.entrySet()) {
                    String key = onlineExtTagEntry.getKey();
                    String value = onlineExtTagEntry.getValue();
                    //缺失节点
                    if (!compareExtTagPair.containsKey(key)) {
                        String[] result = {value,"节点丢失"};
                        resultMap.put(sku+key,result);
                    } else {
                        //比较节点值
                        String info = "";
                        String compValue = compareExtTagPair.get(key);
                        if (value == null){
                            if (compValue != null){
                                String[] result = {"空值",compValue};
                                resultMap.put(sku+key,result);
                            }
                        }else {
                            if (compValue == null){
                                String[] result = {value,"空值"};
                                resultMap.put(sku+key,result);
                            }else if (!value.equals(compValue)){
                                String[] result = {value,compValue};
                                resultMap.put(sku+key,result);
                            }
                        }
                    }
                }
                for (Map.Entry<String, String> compExtTagEntry : compareExtTagPair.entrySet()){
                    String key = compExtTagEntry.getKey();
                    String value = compExtTagEntry.getValue();
                    if (!onlineExtTagPair.containsKey(key)){
                        String[] result = {"未包含节点",value};
                        resultMap.put(sku+key,result);
                    }
                }
            }
        }
        return resultMap;
    }

    /**
     * 比较 TheSkus
     */
    public Map<String,String[]> compareTheSkus(SKU[] onlineSkus, SKU[] compareSkus){
        Map<String,String[]> resultMap = new HashMap<>();
        String template = "SkuId：%s，TheExtTags/%s";
        if (onlineSkus.length != compareSkus.length){
            resultMap.put("sku数量不一致", new String[]{String.valueOf(onlineSkus.length), String.valueOf(compareSkus.length)});
            return resultMap;
        }
        for (SKU onlineSku : onlineSkus){
            //标识两个XML是否有不同的sku
            String skuId = String.valueOf(onlineSku.getId());
            boolean flag = false;
            for (SKU compareSku :compareSkus){
                if (onlineSku.getId() == compareSku.getId()){
                    flag = true;
                    Map<String, String> compareExtTags = compareSku.getExtTags();
                    Map<String, String> onlineExtTags = onlineSku.getExtTags();
                    //轮询线上ExtTags key
                    for (Map.Entry<String, String> onlineExtTagEntry : onlineExtTags.entrySet()) {
                        String key = onlineExtTagEntry.getKey();
                        String value = onlineExtTagEntry.getValue();
                        //缺失节点
                        if (!compareExtTags.containsKey(key)) {
                            String[] result = {value,"节点丢失"};
                            resultMap.put(String.format(template, skuId, key),result);
                        } else {
                            //比较节点值
                            String info = "";
                            String compValue = compareExtTags.get(key);
                            if (value == null){
                                if (compValue != null){
                                    String[] result = {"空值",compValue};
                                    resultMap.put(String.format(template, skuId, key),result);
                                }
                            }else {
                                if (compValue == null){
                                    String[] result = {value,"空值"};
                                    resultMap.put(String.format(template, skuId, key),result);
                                }else if (!value.equals(compValue)){
                                    String[] result = {value,compValue};
                                    resultMap.put(String.format(template, skuId, key),result);
                                }
                            }
                        }
                    }
                    for (Map.Entry<String, String> compExtTagEntry : compareExtTags.entrySet()){
                        String key = compExtTagEntry.getKey();
                        String value = compExtTagEntry.getValue();
                        if (!onlineExtTags.containsKey(key)){
                            String[] result = {"未包含节点",value};
                            resultMap.put(String.format(template, skuId, key),result);
                        }
                    }
                }
            }
            //未包含线上的sku
            if (!flag){
                resultMap.put("缺失节点", new String[]{skuId,"缺失"});
            }
        }
        return resultMap;
    }
    /**
     * 比较sku的ExtTagPair
     * @param onlineCart
     * @param compareCart
     * @return
     */
    public Map<String,String[]> compareExtTagPair(Cart onlineCart, Cart compareCart){
        Map<String,String[]> resultMap = new HashMap<>();

        SKU[] onlineSkus = onlineCart.getTheSkus();
        SKU[] compareSkus = compareCart.getTheSkus();
        if (onlineSkus==null && compareSkus!=null){
            resultMap.put("TheSkus节点数量不一致", new String[]{"0", String.valueOf(compareSkus.length)});
        }else if (onlineSkus!=null && compareSkus==null){
            resultMap.put("TheSkus节点数量不一致", new String[]{String.valueOf(onlineSkus.length), "0"});
        }else if (onlineSkus!=null && compareSkus!=null){
            resultMap.putAll(compareTheSkus(onlineSkus, compareSkus));
        }

        return resultMap;
    }

}
