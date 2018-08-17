package com.mvc.security.procedure.bean;

import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author qiyichen
 * @create 2018/8/17 14:18
 */
@Data
public class Gas {

    private BigDecimal fee;
    private BigInteger gasLimit;
    private BigInteger gasPrice;

}
