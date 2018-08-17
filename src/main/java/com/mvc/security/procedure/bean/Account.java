package com.mvc.security.procedure.bean;

import lombok.Data;

import javax.persistence.Id;
import java.math.BigInteger;
import java.util.Date;

/**
 * account
 *
 * @author qiyichen
 * @create 2018/4/18 14:17
 */
@Data
public class Account {

    @Id
    private BigInteger id;
    private Integer type;
    private String address;
    private String privateKey;
    private Date createdAt;
    private Date updatedAt;
    private Integer isAdmin;
    private BigInteger missionId;

}
