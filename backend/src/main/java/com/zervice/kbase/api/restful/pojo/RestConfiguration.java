package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.pojo.Configuration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

/**
 * rest configuration
 *
 * @author Peng Chen
 * @date 2020/6/17
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestConfiguration {

    @NotBlank(message = "name required")
    String _name;

    @NotBlank(message = "value required")
    String _value;


    public RestConfiguration(Configuration configuration) {
        this._name = configuration.getName();
        this._value = configuration.getValue();
    }
}
