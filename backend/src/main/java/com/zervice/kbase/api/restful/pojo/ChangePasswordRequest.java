package com.zervice.kbase.api.restful.pojo;

import lombok.Getter;
import lombok.Setter;

public class ChangePasswordRequest {
    @Setter
    @Getter
    private String _currentPassword;
    @Setter
    @Getter
    private String _newPassword;
}
