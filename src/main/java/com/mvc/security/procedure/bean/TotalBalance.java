package com.mvc.security.procedure.bean;

import lombok.Data;

import java.math.BigDecimal;

/**
 * TotalBalance
 *
 * @author qiyichen
 * @create 2018/5/4 11:21
 */
@Data
public class TotalBalance {

    private String tokenType;
    private BigDecimal total;
}
