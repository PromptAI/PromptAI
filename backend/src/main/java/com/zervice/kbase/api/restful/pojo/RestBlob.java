package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.pojo.CommonBlob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Peng Chen
 * @date 2020/8/23
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestBlob {
    String _id;

    String _originalFileName;

    public RestBlob(String dbName, CommonBlob blob) {
        this._id = CommonBlob.toExternalId(dbName, blob.getId());
        this._originalFileName = blob.getFileName();
    }
}
