package com.zervice.kbase.database.dao;

import com.alibaba.fastjson.JSONArray;
import com.zervice.common.utils.Base36;
import com.zervice.common.utils.IdGenerator;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.database.criteria.MessageCriteria;
import com.zervice.kbase.database.pojo.Message;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.database.utils.MongoUtils;
import com.zervice.kbase.database.utils.PageResult;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class MessageDao {

    public static Message save(String dbName, Message message) {
        if (StringUtils.isEmpty(message.getId())) {
            message.setId(Base36.encode(IdGenerator.generateId()));
        }
        return MongoUtils.template(dbName).save(message);
    }

    public static PageResult<Message> get(String dbName, MessageCriteria messageCriteria, PageRequest pageRequest) {
        Criteria criteria = Criteria.where("_chatId").is(messageCriteria.getChatId());
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageRequest.getPage(), pageRequest.getSize());

        long count = MongoUtils.template(dbName).count(Query.query(criteria), Message.class);
        List<Message> messages = MongoUtils.template(dbName).find(
                Query.query(criteria)
                        .with(pageable)
                        .with(Sort.by("_time").ascending()), Message.class);
        return PageResult.of(messages, count);
    }

    public static Message get(String dbName, String id) {
        return MongoUtils.template(dbName).findById(id, Message.class);
    }

    public static Message getLastMessage(String dbName, long time) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_time").gt(time));
        query.with(Sort.by("_time").descending()).limit(1);
        return MongoUtils.template(dbName).findOne(query, Message.class);
    }

    public static List<Message> getByChatId(String dbName, String chatId) {
        Criteria criteria = Criteria.where("_chatId").is(chatId);
        Query query = Query.query(criteria);
        return MongoUtils.template(dbName).find(query, Message.class);
    }

    public static Long countByIpAndTimeGt(String dbName, String ip, long time) {
        Criteria criteria = Criteria
                .where("_properties._ip").is(ip)
                .and("_time").gt(time);
        return MongoUtils.template(dbName).count(Query.query(criteria), Message.class);
    }

    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RootQuery {
        private Set<String> _roots;
        private Long _count;
    }

    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopQuery {

        private String _query;

        private Long _count;
    }

}
