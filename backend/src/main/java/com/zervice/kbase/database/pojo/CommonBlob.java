package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.utils.Base36;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Builder
public class CommonBlob {
    public static String separator = "_";
    long _id;
    byte[] _content;
    int _type;

    String _fileName;
    String _md5;

    Prop _properties;

    /**
     * 用户头像
     */
    public static final int TYPE_USER_AVATAR = 1;
    /**
     * kb-pdf节点文件
     */
    public static final int TYPE_COMPONENT_KB_PDF = 2;
    /**
     *  md5Name = DigestUtils.md5DigestAsHex(multipartFile.getInputStream());
     *  知识点附件
     */
    public static final int TYPE_KB_ATTACHMENT = 3;

    public static final int TYPE_MESSAGE_ATTACHMENT = 4;

    public static final String QRCODE_TYPE_WECHAT = "wechat";
    public static Map<String, String> Qrcode2ConfigurationKey = new HashMap<>();

    static {
        Qrcode2ConfigurationKey.put(QRCODE_TYPE_WECHAT, Configuration.GROUP_QRCODE_WECHAT);
    }

    public static String toExternalId(String dbName, long id) {
        return dbName + separator + Base36.encode(id);
    }

    public static Long idFromExternalId(String externalId) {
        try {
            String[] separators = externalId.split(separator);
            // accountDbName_fileId
            if (separators.length == 2) {
                return Base36.decode(separators[1]);
            }
            return Base36.decode(externalId);
        } catch (Exception e) {
            return null;
        }
    }

    public static String dbNameFromExternalId(String externalId) {
        String[] separators = externalId.split(separator);
        if (separators.length != 2) {
            throw new IllegalArgumentException("unknown id - " + externalId);
        }
        return separators[0];
    }

    public static CommonBlob createCommonBlobFromDao(Long id, byte[] content, int type, String fileName,
                                                     String md5,String properties) {
        return CommonBlob.builder()
                .id(id).content(content)
                .type(type).fileName(fileName)
                .properties(JSONObject. parseObject(properties, Prop.class))
                .md5(md5)
                .build();
    }

    @Setter
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop {
        private String _ip;
        private Long _createTime;
    }
}
