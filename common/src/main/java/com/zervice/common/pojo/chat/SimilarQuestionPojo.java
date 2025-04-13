package com.zervice.common.pojo.chat;

import com.zervice.common.utils.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 相似问
 *
 * @author chenchen
 * @Date 2023/7/25
 */
@Setter@Getter
@NoArgsConstructor
public class SimilarQuestionPojo {


    public SimilarQuestionPojo(String id, String query) {
        if (!id.startsWith(Constants.BUTTON_PREFIX)) {
            id = Constants.BUTTON_PREFIX + id;
        }

        this._id = id;
        this._query = query;
    }
    private String _id;

    private String _query;
}
