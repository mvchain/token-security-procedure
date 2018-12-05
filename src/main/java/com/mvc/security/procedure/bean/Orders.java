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
    private String orderId;
    private String tokenType;
    private BigDecimal value;
    private String fromAddress;
    private String toAddress;
    private String feeAddress;
    private Date createdAt;
    private Date updatedAt;
    private BigInteger missionId;
    private String signature;
    private BigInteger nonce;
    //0.collect 1.withdraw 2.approve
    private Integer oprType;
    private BigDecimal gasLimit;
    private BigDecimal gasPrice;
    private String contractAddress;

}
