package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.api.restful.group.Insert;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户更新信息请求
 * @author Peng Chen
 * @date 2020/2/18
 */
@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank(message = "username is required", groups = Insert.class)
    private String _name;

    @NotBlank(message = "phone is required", groups = Insert.class)
    private String _mobile;

    @NotBlank(message = "email is required", groups = Insert.class)
    @Email(message = "email is error format", groups = Insert.class)
    private String _email;
}
