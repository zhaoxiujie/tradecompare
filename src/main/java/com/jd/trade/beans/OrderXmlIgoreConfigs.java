package com.jd.trade.beans;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jd.order.purchase.config.domain.ConfigBase;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author dingmingsong.
 * @date 2019/3/19 10:24
 */
@Getter
@Setter
@XStreamAlias("OrderXmlIgoreConfigs")
public class OrderXmlIgoreConfigs extends ConfigBase {

    private static final long serialVersionUID = 192810733750889271L;

    /**
     * 沃尔玛店铺id
     */
    @Expose
    @XStreamAlias("OrderXmlIgoreList")
    @SerializedName("OrderXmlIgoreList")
    private List<String> orderXmlIgoreList;
}
