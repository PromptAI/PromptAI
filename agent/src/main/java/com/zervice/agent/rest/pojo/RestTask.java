package com.zervice.agent.rest.pojo;

import com.alibaba.fastjson.JSONObject;
import lombok.*;

import java.util.List;

/**
 * @author Peng Chen
 * @date 2022/6/16
 */
@ToString
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RestTask {

    /** 训练 */
    public static final int TYPE_TRAIN = 1;
    /** 部署 */
    public static final int TYPE_DEPLOY = 2;
    private Long _id;

    private String _name;

    private String _agentId;

    private Integer _type;

    private Integer _status;

    private Long _schedule;

    private List<TaskStep> _taskSteps;

    /**
     * used by TaskManager
     */
    private Integer _currentStepIndex = 0;

    public void incStepIndex() {
        _currentStepIndex++;
    }
    private String _lastStepOutput = "";

    public boolean trainTask() {
        return _type != null && _type.equals(TYPE_TRAIN);
    }

    @Setter@Getter
    public static class TaskStep {
        public static final String KEY_LAST_OUT_PUT = "_lastOutput";
        private String _type;

        private Integer _retryTimes = 0;

        /**
         * contains a special key：
         *  _lastOutput： the result of last step
         *
         *
         *  it may empty ""
         *   For DOWNLOAD File: the full path of new file
         *   For TRAIN_MODEL: the full path of new model....
         *
         *
         */
        private JSONObject _properties;

        public void storeLastOutput(String result) {
            _properties.put(KEY_LAST_OUT_PUT, result);
        }

        public JSONObject toJSON() {
            return JSONObject.parseObject(JSONObject.toJSONString(this));
        }
    }




}
