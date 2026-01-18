package com.bookkeeping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAccountRequest {
    
    @NotBlank(message = "账户名称不能为空")
    @Size(max = 100, message = "账户名称长度不能超过100个字符")
    private String name;
    
    @NotBlank(message = "账户类型不能为空")
    private String type;
    
    private String note;
}
