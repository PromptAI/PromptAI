package com.zervice.kbase.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 账户激活服务 - 通过邮件发送激活连接进行激活
 */
@Log4j2
public class AccountActiveService {

    private AccountActiveService(){
        init();
    }

    private static AccountActiveService _instance = new AccountActiveService();

    private static Map<String, ActiveInfo> _activeInfos = new HashMap<>();

    public void init() {
        ThreadFactory factory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("sync-Agent-clients")
                .build();
        Executors.newSingleThreadScheduledExecutor(factory)
                .scheduleAtFixedRate(this::_expireActiveInfo, 5, 5, TimeUnit.SECONDS);
    }

    private void _expireActiveInfo() {
        if (_activeInfos.isEmpty()) {
            return;
        }
        _activeInfos.values().removeIf(ActiveInfo::expired);
    }


    /**
     * 有效期
     */
    public static long EXPIRE_IN_MILLIS = TimeUnit.DAYS.toMillis(1);

    public static AccountActiveService getInstance() {
        return _instance;
    }

    /**
     * 申请注册
     *
     * @param account 登录账户，目前仅支持邮件
     * @param code    code
     */
    public static void registry(String account, String code) {
        ActiveInfo activeInfo = ActiveInfo.factory(account, code);
        _activeInfos.remove(account);
        _activeInfos.put(account, activeInfo);
    }

    @Builder
    @Setter@Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveInfo {
        private String _account;

        private String _code;

        private Long _createTime;

        public boolean auth(String code) {
            return _code.equals(code);
        }

        public boolean expired() {
            return System.currentTimeMillis() - EXPIRE_IN_MILLIS > _createTime;
        }

        public static ActiveInfo factory(String account, String code) {
            return ActiveInfo.builder()
                    .account(account)
                    .code(code).createTime(System.currentTimeMillis())
                    .build();
        }

    }
}
