package com.jd.trade.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class sendPay {

    public String setSendPay(String s,int num)
    {
        List l=new ArrayList();
        for(int i=0;i<s.length();i++)
        {
           char[] c = s.toCharArray();
           l.add(c[i]);

        }
        return (l.get(num-1)).toString();
    }
//    public List yuanshidata(String s)
//    {
//        List l=new ArrayList();
//        for(int i=0;i<s.length();i++)
//        {
//            char[] c = s.toCharArray();
//            l.add(c[i]);
//
//        }
//        return l;
//    }
//    public List bijiao(String s)
//    {
//        List l1=new ArrayList();
//        for(int i=0;i<s.length();i++)
//        {
//            char[] c = s.toCharArray();
//            l1.add(c[i]);
//
//        }
//        return l1;
//    }
//    public String data(List a,List b)
//    {
//        String s;
//        List<sendpaybijiao> la=new ArrayList<sendpaybijiao>();
//        for(int i=0;i<a.size();i++)
//        {
//            sendpaybijiao sa=new sendpaybijiao();
//            if(a.get(i)!=b.get(i))
//            {
//                sa.setkey(i);
//                sa.setyuanshidata(a.get(i).toString());
//                sa.setbijiaodata(b.get(i).toString());
//                la.add(sa);
//            }
//        }
////        for(int i=0;i<la.size();i++)
////        {
////            sendpaybijiao sb=new sendpaybijiao();
////            sb=la.get(i);
////           s=s+sb.getkey()+sb.getyuanshidata()+sb.getbijiaodata()+"/";
////        }
//        return s;
//    }
    public static void main(String[] args)
    {
        sendPay s=new sendPay();
        System.out.println(s.setSendPay("23456789",3));
    }


}
