package com.example.bishe.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class UpdatePwdDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
