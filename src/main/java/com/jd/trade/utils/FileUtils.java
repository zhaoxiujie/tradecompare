package com.jd.trade.utils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lienpeng
 * @date: 2019/12/9 10:25
 */
@Component
public class FileUtils {
    public String stringToXML(String string){
        SAXReader saxReader = new SAXReader();
        Document document;
        try {
            document = saxReader.read(new ByteArrayInputStream(string.getBytes("UTF-8")));
            OutputFormat format = OutputFormat.createPrettyPrint();

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream(2048);
            XMLWriter writer = new XMLWriter(byteOut,format);
            writer.write(document);
            writer.close();
            return byteOut.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    /**
     * 格式化map，输出差异点
     */
    public String mapToString(Map<String, String[]> map){
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String[]> entry : map.entrySet()){
            String temp = String.format("【%s】\n线上的值为：%s，\n组件化值为：%s。",entry.getKey(),entry.getValue()[0],entry.getValue()[1]);
            String htmlString = temp.replace("\n","<br >");
            result.append(htmlString).append("<br><hr>");
        }
        return result.toString();
    }

    /***
     *比较XML
     * @param onlineXML
     * @param compXML
     * @return
     */
    public Map<String,String[]> compareXML(String onlineXML,String compXML){
        Map<String,String[]> contents = new HashMap<>();
        //比较其他节点
        Diff myDiff = DiffBuilder.compare(onlineXML).withTest(compXML).checkForSimilar().ignoreElementContentWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
//                .withComparisonController(ComparisonControllers.Default)
                .normalizeWhitespace()
//                .withNodeMatcher(new OrderNodeMatcher())
//                .withNodeMatcher(nodeMatcher)
                // 默认的格式化输出，可以自定义输出格式，实现ComparisonFormatter接口
                .withComparisonFormatter(new DefaultComparisonFormatter()).build();
        Iterable<Difference> differences = myDiff.getDifferences();
        for (Difference difference : differences) {
            if (ComparisonResult.DIFFERENT == difference.getResult()) {
                Comparison comparison = difference.getComparison();
//                final ComparisonType type = comparison.getType();
//                String description = type.getDescription();
                final Comparison.Detail controlDetails = comparison.getControlDetails();
                final Comparison.Detail testDetails = comparison.getTestDetails();
                String controlXPath = controlDetails.getXPath();
                String testXPath = testDetails.getXPath();
                Node controlTarget = controlDetails.getTarget();
                Node testTarget = testDetails.getTarget();
                if (controlXPath != null && controlXPath.contains("/Order") && controlXPath.contains("ExtTagPair")) {
                    // 忽略订单扩展节点
                    continue;
                }
                if (controlXPath == null || controlTarget == null){
                    String[] result = {"未包含节点","多余节点"};
                    contents.put("^"+testXPath,result);
                }
                //暂时去掉 && testTarget.hasChildNodes()
                else if (controlTarget.hasChildNodes() && testTarget != null
                        && testTarget.hasChildNodes()) {
                    // 忽略
                } else if (!controlTarget.hasChildNodes() && testTarget == null) {
                    // 忽略
                } else if (controlTarget.hasChildNodes() && testTarget == null) {
                    String[] result = {"","节点丢失"};
                    int length = controlTarget.getChildNodes().getLength();
                    if (length>1){
                        StringBuilder temp = new StringBuilder("包含节点：");
                        for (int i=0;i<length;i++){
                            temp.append(controlTarget.getChildNodes().item(i).getNodeName()).append(";");
                        }
                        result[0]=temp.toString();
                    }else {
                        String nodeValue = controlTarget.getFirstChild().getNodeValue();
                        if (nodeValue == null){
                            result[0]= "包含节点："+controlTarget.getFirstChild().getNodeName();
                        }else {
                            result[0] = nodeValue;
                        }

                    }
                    contents.put("^"+controlXPath,result);
                } else {
                    String[] result = new String[2];
                    result[0] = (controlTarget.getNodeValue());
                    result[1] = (testTarget == null ? "null" : testTarget.getNodeValue());
                    contents.put(controlXPath,result);
                }
            }
        }
        return contents;
    }}
