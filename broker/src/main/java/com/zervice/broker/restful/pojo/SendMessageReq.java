package com.zervice.broker.restful.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Peng Chen
 * @date 2022/7/7
 */
@Setter @Getter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageReq {
    private String _sender;

    private String _message;

    private String _showMessage;
}
