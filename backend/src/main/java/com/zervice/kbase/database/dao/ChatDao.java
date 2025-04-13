package com.zervice.kbase.database.dao;

import com.zervice.common.utils.Base36;
import com.zervice.common.utils.IdGenerator;
import com.zervice.kbase.api.restful.pojo.ChatCriteria;
import com.zervice.kbase.api.restful.pojo.PageRequest;
import com.zervice.kbase.database.criteria.RpcChatCriteria;
import com.zervice.kbase.database.pojo.Chat;
import com.zervice.kbase.database.utils.MongoUtils;
import com.zervice.kbase.database.utils.PageResult;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Log4j2
public class ChatDao {

    public static final String TABLE_NAME = "chats";

    public static Chat get(String dbName, String id) {
        return MongoUtils.template(dbName).findById(id, Chat.class);
    }

    public static Chat save(String dbName, Chat chat) {
        if (StringUtils.isEmpty(chat.getId())) {
            chat.setId(Base36.encode(IdGenerator.generateId()));
        }
        return MongoUtils.template(dbName).save(chat);
    }

    public static void updateProp(String dbName, String id, Chat.Prop prop) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("_properties", prop);
        MongoUtils.template(dbName).updateFirst(query, update, Chat.class);
    }

    public static PageResult<Chat> get(String dbName, RpcChatCriteria chatCriteria, PageRequest pageRequest) {
        // project must not be null
        String projectId = chatCriteria.getProjectId();
        Map<String, String> propertiesFilter = chatCriteria.getProperties();
        Long startTime = chatCriteria.getStartTime();
        Long endTime = chatCriteria.getEndTime();

        Criteria criteria = Criteria.where("_properties._projectId").is(projectId);
        criteria.and("_properties._hasMessage").is(true);

        if (CollectionUtils.isNotEmpty(chatCriteria.getRoots())) {
            criteria.and("_properties._rootComponentIds").in(chatCriteria.getRoots());
        }

        if (propertiesFilter != null && !propertiesFilter.isEmpty()) {
            for (Map.Entry<String, String> entry : propertiesFilter.entrySet()) {
                criteria.and("_properties." + entry.getKey()).is(entry.getValue());
            }
        }

        if (startTime != null && endTime != null) {
            criteria.and("_visitTime").gte(startTime).lte(endTime);
        } else if (startTime != null) {
            criteria.and("_visitTime").gte(startTime);
        } else if (endTime != null) {
            criteria.and("_visitTime").lte(endTime);
        }

        long count = MongoUtils.template(dbName).count(Query.query(criteria), Chat.class);

        Query pageQuery = Query.query(criteria).with(pageRequest.toMongoPageable());
        List<Chat> chats = MongoUtils.template(dbName).find(pageQuery, Chat.class);
        return PageResult.of(chats, count);
    }

    /**
     * internal api
     */
    public static PageResult<Chat> get(String dbName, ChatCriteria chatCriteria, PageRequest pageRequest) {
        Criteria criteria = new Criteria();
        Query query = new Query(criteria);

        List<Criteria> criterias = new ArrayList<>();
        criterias.add(Criteria.where("_properties._hasMessage").is(chatCriteria.getHasMessage()));

        if (StringUtils.isNotBlank(chatCriteria.getPublishedProjectId())) {
            criterias.add(Criteria.where("_properties._publishedProjectId").is(chatCriteria.getPublishedProjectId()));
        }

        if (StringUtils.isNotBlank(chatCriteria.getProjectId())) {
            criterias.add(Criteria.where("_properties._projectId").is(chatCriteria.getProjectId()));
        }

        if (CollectionUtils.isNotEmpty(chatCriteria.getRootComponentIds())) {
            criterias.add(Criteria.where("_properties._rootComponentIds").in(chatCriteria.getRootComponentIds()));
        }

        if (CollectionUtils.isNotEmpty(chatCriteria.getEvaluates())) {
            criterias.add(Criteria.where("_properties._evaluates").in(chatCriteria.getEvaluates()));
        }

        if (StringUtils.isNotBlank(chatCriteria.getScene())) {
            criterias.add(Criteria.where("_properties._scene").is(chatCriteria.getScene()));
        }

        criteria.andOperator(criterias.toArray(new Criteria[0]));
        long count = MongoUtils.template(dbName).count(query, Chat.class);

        query.with(pageRequest.toMongoPageable());

        List<Chat> chats = MongoUtils.template(dbName).find(query, Chat.class);
        return PageResult.of(chats, count);
    }

    public static Chat getLastPublishProjectChat(String dbName, String publishedProjectId, long time) {

        Criteria criteria = new Criteria();
        Query query = new Query(criteria);

        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("_properties._hasMessage").is(true));
        criteriaList.add(Criteria.where("_properties._publishedProjectId").is(publishedProjectId));
        criteriaList.add(Criteria.where("_visitTime").gt(time));
        query.with(Sort.by("_visitTime").descending()).limit(1);
        criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        return MongoUtils.template(dbName).findOne(query, Chat.class);
    }

    public static Long countByDistinctIp(String dbName) {
        // flowing code equals : db.chats.distinct("_properties._ip").length
        GroupOperation groupOperation = Aggregation.group("_properties._ip");
        CountOperation countOperation = Aggregation.count().as("total");
        Aggregation aggregation = Aggregation.newAggregation(groupOperation, countOperation);

        Document result = MongoUtils.template(dbName).aggregate(aggregation, TABLE_NAME, Document.class)
                .getUniqueMappedResult();
        return Objects.nonNull(result) ? result.getInteger("total").longValue() : 0;
    }


    public static Long countByProjectId(String dbName, String projectId) {
        Criteria criteria = Criteria
                .where("_properties._projectId").is(projectId);
        return MongoUtils.template(dbName).count(Query.query(criteria), Chat.class);
    }

    public static Long countByProjectIdAndTimeGt(String dbName, String projectId, long time) {
        Criteria criteria = Criteria
                .where("_properties._projectId").is(projectId)
                .and("_visitTime").gt(time);
        return MongoUtils.template(dbName).count(Query.query(criteria), Chat.class);
    }


    public static Long countByProjectIdAndTimeBetween(String dbName, String projectId,
                                                      long startTime, long endTime) {
        Criteria criteria = Criteria
                .where("_properties._projectId").is(projectId)
                .and("_visitTime").gte(startTime).lte(endTime);
        return MongoUtils.template(dbName).count(Query.query(criteria), Chat.class);
    }

    @Builder
    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Flow {
        private String _id;
        private String _ip;
        private Long _visitTime;
        private Map<String /*entity id*/, Chat.FilledSlot> _filledSlots;

        public static Flow factory(Chat chat) {
            Chat.Prop prop = chat.getProperties();
            return Flow.builder()
                    .id(chat.getId()).ip(prop.getIp())
                    .visitTime(chat.getVisitTime())
                    .filledSlots(prop.getFilledSlots())
                    .build();
        }
    }
}
