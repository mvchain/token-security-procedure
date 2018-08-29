package com.mvc.security.procedure.bean.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @author qiyichen
 * @create 2018/4/18 15:19
 */
@Data
public class NewAccountDTO implements Serializable {
    private static final long serialVersionUID = 7530500523300418101L;
    @NotNull(message = "令牌类型不能为空")
    @Min(value = 1, message = "生成数量不能为0")
    private Integer number;
    @NotNull(message = "令牌类型不能为空")
    @Size(min = 1, message = "令牌类型不能为空")
    private String tokenType;

    public String getTokenType() {
        if (null != this.tokenType) {
            return tokenType.toUpperCase();
        }
        return tokenType;
    }
}
