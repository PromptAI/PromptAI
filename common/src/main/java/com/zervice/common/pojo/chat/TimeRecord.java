package com.zervice.common.pojo.chat;

import com.zervice.common.utils.TimeUtils;
import lombok.*;

/**
 * message time record
 *
 * @author chenchen
 * @Date 2023/8/30
 */
@Builder
@Setter@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TimeRecord {

    public static final String TYPE_REQUEST_KB_COMPONENT_WITH_CHILDREN = "req_kb_component_with_children";
    public static final String TYPE_REQUEST_KB_COMPONENT = "req_kb_component";
    public static final String TYPE_REQUEST_KB_COMPONENT_ID = "req_kb_component_id";
    public static final String TYPE_REQUEST_KB_ENTITY = "req_kb_entity";
    public static final String TYPE_REQUEST_KB_QUERY_INTENT= "req_kb_query_intent";
    public static final String TYPE_REQUEST_KB_SIMILAR_QUESTIONS= "req_kb_query_similar_questions";


    public static final String TYPE_REQUEST_KBQA = "req_kbqa";
    public static final String TYPE_REQUEST_KBQA_SSE = "req_kbqa_sse";

    public static final String TYPE_REQUEST_MICA = "req_mica";



    public static TimeRecord start(String type) {
        return new TimeRecord(type, true);
    }

    public static TimeRecord end(String type) {
        return new TimeRecord(type, false);
    }

    public TimeRecord(String type, boolean start) {
        String suffix = start ? "_start" : "_end";
        this._name = type + suffix;
        this._time = TimeUtils.format(TimeUtils.PATTERN_DETAILS);
    }

    private String _name;

    private String _time;

}
