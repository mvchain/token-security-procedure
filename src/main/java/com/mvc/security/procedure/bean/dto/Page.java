package com.mvc.security.procedure.bean.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * page info
 *
 * @author qiyichen
 * @create 2018/4/18 15:28
 */
@Data
public class Page implements Serializable {

    private static final long serialVersionUID = -8473621139755440807L;

    private Integer pageSize;
    private Integer pageNum;
    private String orderBy;
}
