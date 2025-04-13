package com.zervice.kbase.api.restful.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCriteria {
    private String _name;
    private String _type;
    private String _status;
    private Boolean _active;

}
