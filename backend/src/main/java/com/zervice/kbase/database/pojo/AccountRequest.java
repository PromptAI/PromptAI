package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountRequest {
    private Long _id;
    private String _status;
    private String _company;
    private String _name;
    private String _domain;
    private String _mxDomain;
    private String _creator;
    private JSONObject _properties;
}
