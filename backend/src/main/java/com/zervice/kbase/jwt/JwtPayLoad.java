package com.zervice.kbase.jwt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * jwt 存储内容
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtPayLoad {

    /**
     * login timestamp
     */
    private long _t;

    /**
     * external Account Id
     */
    private String _aid;

    /**
     * External user ID
     */
    private String _uid;

    /**
     * Readable user name
     */
    private String _user;

    /**
     * 构造以当前时间登录的Payload
     *
     * @param aid account id
     * @param uid user id
     * @return payload
     */
    public static JwtPayLoad of(String aid, String uid, String user) {
        JwtPayLoad payLoad = new JwtPayLoad();
        payLoad.setAid(aid);
        payLoad.setUid(uid);
        payLoad.setUser(user);
        payLoad.setT(System.currentTimeMillis());
        return payLoad;
    }
}
