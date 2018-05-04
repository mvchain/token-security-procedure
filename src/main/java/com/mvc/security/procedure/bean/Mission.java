package com.mvc.security.procedure.bean;

import lombok.Data;

import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * Mission
 *
 * @author qiyichen
 * @create 2018/4/18 15:26
 */
@Data
public class Mission {

    @Id
    private BigInteger id;
    private Integer type;
    private Integer total;
    private Integer complete;
    private Date createdAt;
    private Date updatedAt;
    private String tokenType;
    @Transient
    private List<TotalBalance> totalBalance;

}
