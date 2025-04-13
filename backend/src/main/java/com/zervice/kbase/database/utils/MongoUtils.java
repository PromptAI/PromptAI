package com.zervice.kbase.database.utils;

import com.google.common.base.Preconditions;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.zervice.common.utils.LayeredConf;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Peng Chen
 * @date 2020/12/16
 */
@Component
public class MongoUtils {

    private static final MongoClient mongoClient;

    static {
        String mongoUri = LayeredConf.getString("mongodb.uri", null);
        Preconditions.checkNotNull(mongoUri, "mongodb.uri is not set");
        mongoClient = MongoClients.create(mongoUri);
    }

    private  static Map<String, MongoTemplate> mongoTemplateMap = new ConcurrentHashMap<>(16);

    public static MongoTemplate template(String dbName) {
        MongoTemplate mongoTemplate = mongoTemplateMap.get(dbName);
        if (mongoTemplate == null) {

            synchronized (mongoTemplateMap) {
                mongoTemplate = mongoTemplateMap.get(dbName);
                if (mongoTemplate == null) {
                    mongoTemplate = new MongoTemplate(mongoClient, dbName);
                    mongoTemplateMap.put(dbName, mongoTemplate);
                    return mongoTemplate;
                }

                return mongoTemplate;
            }
        }

        return mongoTemplate;
    }
}
