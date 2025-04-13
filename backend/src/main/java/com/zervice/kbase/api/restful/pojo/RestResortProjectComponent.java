package com.zervice.kbase.api.restful.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

/**
 * 节点重排序
 *
 * @author chen
 * @date 2022/11/29
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestResortProjectComponent {
    @NotBlank
    private String _c1;
    @NotBlank
    private String _c2;
}
