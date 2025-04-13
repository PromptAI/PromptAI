package com.zervice.kbase.service.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.kbase.api.restful.pojo.VerificationCode;
import com.zervice.kbase.service.VerificationCodeService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Zheng Jie
 * @date 2018-12-26
 */
@Service
@Log4j2
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final Integer expirationInSec = 120;
    private ConcurrentHashMap<String /*UUID*/, VerificationCode> existsCodes = new ConcurrentHashMap<>();


    public VerificationCodeServiceImpl() {
        scheduleRemoveExpireCodeTask();
    }

    private void scheduleRemoveExpireCodeTask() {
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("cleanup-expirecode").build()).scheduleAtFixedRate(() -> {
            try {
                Iterator<Map.Entry<String /*UUID*/, VerificationCode>> iterator = existsCodes.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String /*UUID*/, VerificationCode> code = iterator.next();
                    if (Math.abs(System.currentTimeMillis() - code.getValue().getCreateTimeInMills()) > expirationInSec * 1000) {
                        iterator.remove();
                    }
                }
            }
            catch (Exception e) {
                LOG.error("Fail to remove expiring codes", e);
            }
        }, 10, expirationInSec, TimeUnit.SECONDS);
    }

    @Override
    public boolean validated(String uuid, VerificationCode code) {
        VerificationCode correctCode = existsCodes.get(uuid);
        if (correctCode == null || !correctCode.getCode().equalsIgnoreCase(code.getCode())) {
            return false;
        }
        else {
            existsCodes.remove(uuid);
            return true;
        }
    }

    @Override
    public void registerNewCode(String uuid, VerificationCode code) {
        existsCodes.put(uuid, code);
    }


}
