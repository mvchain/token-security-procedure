package com.mvc.security.procedure.bean;

import lombok.Data;

import java.math.BigInteger;

/**
 * TotalBalance
 *
 * @author qiyichen
 * @create 2018/5/4 11:21
 */
@Data
public class TotalBalance {


    private String tokenType;

    private BigInteger total;

}
