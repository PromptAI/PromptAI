package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.utils.Constants;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * This step not a table but a part of
 *
 * @author Peng Chen
 * @date 2022/6/16
 */
@Log4j2
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStep {

    public static final String TYPE_DOWNLOAD = Constants.TYPE_DOWNLOAD;
    public static final String TYPE_MOVE_FILE_TO_DIR = Constants.TYPE_MOVE_FILE_TO_DIR;
    public static final String TYPE_TRAIN_MODEL = Constants.TYPE_DEPLOY;
    public static final String TYPE_UPLOAD = Constants.TYPE_UPLOAD;
    public static final String TYPE_UNLOAD_MODEL = Constants.TYPE_UNLOAD_MODEL;
    public static final String TYPE_REPLACE_MODEL = Constants.TYPE_REPLACE_MODEL;
    public static final String TYPE_CHECK_MODEL = Constants.TYPE_CHECK_MODEL;
    public static final String TYPE_REPORT_MODEL = Constants.TYPE_REPORT_MODEL;


    public static final String TASK_RECEIVED_CONTENT = "task received";
    public static final String TASK_START_CONTENT = "start run task";
    public static final String TASK_FINISH_CONTENT = "Finish";

    private String _type;

    private Integer _retryTimes;

    private JSONObject _properties;

    /**
     * render properties variables to real data
     * For example:
     * {
     * 			"type": "DOWNLOAD",
     * 			"retryTimes": 3,
     * 			"properties": {
     * 				"url": "{model_url}",
     * 				"fileName": "xxx.zip",
     * 				"md5": "{model_md5}"
     *           }
     * }
     *
     * Params Map :
     *
     * {
     *     "{model_url}" :"http://www.xx.com/file/xx.zip",
     *     "{model_md5}" : "xxxxxxx"
     *
     * }
     *
     * After render :
     *
     * {
     * 			"type": "DOWNLOAD",
     * 			"retryTimes": 3,
     * 			"properties": {
     * 				"url": "http://www.xx.com/file/xx.zip",
     * 				"fileName": "xxx.zip",
     * 				"md5": "xxxxxxx"
     *           }
     * }
     *
     * For now，only render the values in properties
     */
    public TaskStep render(Map<String, String> params) {
        if (_properties == null || _properties.isEmpty()) {
            return this;
        }

        for (String key : _properties.keySet()) {
            Object value = _properties.get(key);
            if (value instanceof String) {
                String vStr = (String) value;
                if (params.containsKey(vStr)) {
                    String newVal = params.get(vStr);

                    // render
                    _properties.put(key, newVal);
                    LOG.info("render step for key:{} with :{}", key, newVal);
                }
            }
        }

        return this;
    }
}
