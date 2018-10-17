package com.mvc.security.procedure.bean;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author qiyichen
 * @create 2018/10/9 17:22
 */
@Data
public class OrderEntity {

    private String sign;
    private String jsonStr;
    private BigDecimal gasPrice;
}
