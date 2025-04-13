package com.zervice.kbase.api.rpc.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.database.pojo.Chat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * RPC chat
 * @author chenchen
 * @Date 2023/12/12
 */
@Setter@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RpcChat {

    private String _id;

    private Long _visitTime;

    private Prop _properties;

    @Setter@Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class  Prop {


        private String _ip;
        /**
         * {@link IpPojo}
         */
        private JSONObject _ipExtra;

        /**
         * project locale
         */
        private String _locale;

        /**
         * user agent from http.request
         */
        private JSONObject _userAgent;

        /**
         * 对话过程中填充的变量
         */
        private Map<String /*entity id*/, Chat.FilledSlot> _filledSlots;

        /**
         * 创建对话时需要填充到上下文的slot - 从外部系统来
         */
        private Map<String, Object> _slots;

        /**
         * 需要存储的变量 - 从外部系统来
         */
        private Map<String, Object> _variables;

        /**
         * 使用场景
         */
        private String _scene;
    }
}
