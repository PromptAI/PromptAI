package com.zervice.kbase.jwt;

import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.impl.PublicClaims;
import com.zervice.kbase.ZBotConfig;
import com.zervice.kbase.database.pojo.User;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author :   chen
 * date:     2020/2/4 12:39
 * description: token服务
 */
@Log4j2
public class JwtTokenUtil {

    /**
     * 过期时间目前是1天
     */
    public static final long EXPIRE_MILES = 24 * 60 * 60 * 1000;

    /**
     * token类型
     */
    private static final String HEADER_ALG = "HS256";

    /**
     * 加密算法
     */
    private static final String HEADER_TYPE = "jwt";

    /**
     * token 前缀
     */
    public static final String PREFIX = "Bearer ";


    /**
     * 自动生成token
     *
     * @param payload 自定义字段
     * @return token
     */
    public static String generate(JwtPayLoad payload) {

        //第二天过期
        Date exp = new Date(System.currentTimeMillis() + EXPIRE_MILES);

        JWTCreator.Builder builder = JWT.create()
                .withHeader(getHeader())
                .withIssuer(ZBotConfig.JWT_PAYLOAD_ISS)
                //jwt的签发时间
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(exp)
                .withJWTId(payload.getUid());

        try {
            builder = buildClaim(payload, builder);
        } catch (IllegalAccessException e) {
            LOG.error(e.getMessage());
            throw new RestException(StatusCodes.InternalError, "token生成异常");
        }

        return PREFIX + builder.sign(Algorithm.HMAC256(ZBotConfig.JWT_SECRET));
    }

    /**
     * 根据token解析出Payload数据
     *
     * @param token token
     * @return Payload
     */
    public static String getPayloadStringByToken(String token) {

        String[] split = token.split("\\.");
        String payLoadString = split[1];
        byte[] bytes = Base64.decodeBase64(payLoadString);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 解析token并获取LoginUser对象
     *
     * @param token token
     * @return
     */
    public static JwtPayLoad getPayLoadByToken(String token) {
        String payload = getPayloadStringByToken(token.replace(PREFIX, ""));
        // 常用字段
        JwtPayLoad payLoad = JSONObject.parseObject(payload, JwtPayLoad.class);
        return payLoad;
    }

    public static Long getUserIdByToken(String token) {
        String userId = get(token).getUid();
        return User.fromExternalId(userId);
    }

    /**
     * 从token中获取用户信息《会校验token有效性》
     *
     * @param token token
     * @return user
     */
    public static JwtPayLoad get(String token) {

        //check token是否正常
        try {
            token = token.replace(PREFIX, "");

            //verify
            Algorithm algorithm = Algorithm.HMAC256(ZBotConfig.JWT_SECRET);
            JWTVerifier jwtVerifier = JWT.require(algorithm).build();
            jwtVerifier.verify(token);

            //payload
            return JwtTokenUtil.getPayLoadByToken(token);
        } catch (Exception ex) {
            LOG.error("Token校验失败", ex);
            throw new RestException(StatusCodes.Unauthorized, "Token invalid");
        }
    }


    /**
     * jwt 头信息
     *
     * @return
     */
    private static Map getHeader() {
        Map<String, Object> headerClaims = new HashMap<>(2);
        headerClaims.put(PublicClaims.ALGORITHM, HEADER_ALG);
        headerClaims.put(PublicClaims.TYPE, HEADER_TYPE);
        return headerClaims;
    }

    /**
     * 通过反射设置token的字段
     *
     * @param payload 自定义内容
     * @param builder jwt 构造器
     * @return JWTCreator.Builder
     * @throws IllegalAccessException
     */
    private static JWTCreator.Builder buildClaim(JwtPayLoad payload, JWTCreator.Builder builder) throws IllegalAccessException {

        // 反射获取对象全部的字段
        Field[] fieldList = payload.getClass().getDeclaredFields();
        // 逐个加入   builder中
        for (Field field : fieldList) {
            // 字段名
            String fieldName = field.getName();

            // 获取字段的值
            field.setAccessible(true);
            Object fieldValue = field.get(payload);

            if (fieldValue == null) {
                continue;
            }

            // 设值
            builder = setClaim(builder, fieldName, fieldValue);

        }

        return builder;
    }

    /**
     * 设值
     *
     * @param builder    builder
     * @param fieldName  字段名称
     * @param fieldValue 字段值
     * @return builder
     */
    private static JWTCreator.Builder setClaim(JWTCreator.Builder builder, String fieldName, Object fieldValue) {
        // Integer
        if (fieldValue instanceof Integer) {
            builder.withClaim(fieldName, (Integer) fieldValue);
        } else if (fieldValue instanceof Long) {
            // Integer
            builder.withClaim(fieldName, (Long) fieldValue);
        } else if (fieldValue instanceof Double) {
            // Double
            builder.withClaim(fieldName, (Double) fieldValue);
        } else if (fieldValue instanceof Boolean) {
            // Boolean
            builder.withClaim(fieldName, (Boolean) fieldValue);
        } else if (fieldValue instanceof String) {
            // String
            builder.withClaim(fieldName, String.valueOf(fieldValue));
        } else if (fieldValue instanceof Date) {
            // Date
            builder.withClaim(fieldName, (Date) fieldValue);
        } else if (fieldValue instanceof String[]) {
            // String []
            builder.withArrayClaim(fieldName, (String[]) fieldValue);
        } else {
            // 不支持其他类型
            LOG.error("token生成异常:  字段类型不支持！");
            throw new RestException(StatusCodes.InternalError);
        }
        return builder;
    }


}
