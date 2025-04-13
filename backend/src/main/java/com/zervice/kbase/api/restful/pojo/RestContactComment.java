package com.zervice.kbase.api.restful.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author admin
 * @date 2022/11/7
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestContactComment {
    private Long _id;
    @NotBlank
    private String _name;
    private String _email;
    private String _mobile;
    @NotBlank
    private String _content;



}
