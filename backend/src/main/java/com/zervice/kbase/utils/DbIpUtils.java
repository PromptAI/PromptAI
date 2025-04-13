package com.zervice.kbase.utils;

import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * 查询ip对应的信息
 *
 * @author chen
 * @date 2022/10/27
 */
@Log4j2
public class DbIpUtils {

    private static DatabaseReader dbReader;
    static {
        try {
            Resource database = new ClassPathResource("ip/dbip-city-lite-latest.mmdb");
            dbReader = new DatabaseReader
                    .Builder(database.getInputStream())
                    .build();
        } catch (Exception e) {
            LOG.error("[init db-ip fail:{}]", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static final LoadingCache<String, CityResponse> ipCache = CacheBuilder.newBuilder()
            .expireAfterAccess(60 * 10, TimeUnit.SECONDS)
            .build(new CacheLoader<String, CityResponse>() {
                @Override
                public CityResponse load(String key) throws Exception {
                    return null;
                }
            });

    /**
     * ipv4 & ipv6 supported
     * // TODO: 这做一个超时规则...
     */
    public static CityResponse query(String ip){
        Stopwatch stopwatch = Stopwatch.createStarted();
        if (StringUtils.isBlank(ip)) {
            LOG.error("[query ip fail, ip is black]");
            return null;
        }

        // 82.149.137.6, 127.0.0.1  经过代理后有多个ip
        ip = ip.split(",")[0];

        CityResponse cityResponse = ipCache.getIfPresent(ip);
        if (cityResponse != null) {
            LOG.info("[query ip elapsed:{}]", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return cityResponse;
        }

        try {
            CityResponse response = dbReader.city(InetAddress.getByName(ip));
            ipCache.put(ip, response);

            LOG.info("[query ip elapsed:{}]", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return response;
        } catch (Exception e) {
            LOG.error("[query ip:{} fail:{}, elapsed:{}]", ip, e.getMessage(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return null;
        }
    }
}
