package com.mvc.security.procedure.bean;

import lombok.Data;

import javax.persistence.Id;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Orders
 *
 * @author qiyichen
 * @create 2018/4/18 15:05
 */
@Data
public class Orders {
    @Id
    private BigInteger id;
    private BigInteger nonce;
    private String orderId;
    private String tokenType;
    private BigDecimal value;
    private String fromAddress;
    //btc没有toAddress,这个字段用来存listUnspent的json
    private String toAddress;
    private Date createdAt;
    private Date updatedAt;
    private BigInteger missionId;
    private String signature;
    private BigDecimal fee;

    public BigDecimal getValue() {
        return null != value ? value : BigDecimal.ZERO;
    }

}
