package com.mvc.security.procedure.bean;

import lombok.Data;

import java.math.BigInteger;

/**
 * @author qiyichen
 * @create 2018/12/3 14:06
 */
@Data
public class BlockSign {
    private BigInteger id;
    private Integer oprType;
    private String orderId;
    private String sign;
    private String result;
    private Integer status;
    private String hash;
    private Long startedAt;
    private String tokenType;
    private String contractAddress;
    private String fromAddress;
    private String toAddress;

}
