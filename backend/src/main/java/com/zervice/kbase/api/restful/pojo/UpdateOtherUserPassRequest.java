package com.zervice.kbase.api.restful.pojo;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

/**
 * Update other user password request
 *
 * @author Peng Chen
 * @date 2020/2/18
 */
@Setter
@Getter
public class UpdateOtherUserPassRequest {

    // user id
    private long _id;
    // the pass word
    @NotBlank
    private String _newPass;
}
