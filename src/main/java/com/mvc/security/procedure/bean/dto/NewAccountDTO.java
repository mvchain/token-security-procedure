package com.mvc.security.procedure.bean.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author qiyichen
 * @create 2018/4/18 15:19
 */
@Data
public class NewAccountDTO implements Serializable {
    private static final long serialVersionUID = 7530500523300418101L;

    private Integer number;
    private String tokenType;

}
