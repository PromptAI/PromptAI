package com.zervice.kbase.database.utils;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * Dao page query result
 *
 * @author Peng Chen
 * @date 2020/2/15
 */
@Setter
@Getter
public class PageResult<T> {

    private List<T> _data;

    private Long _totalCount;

    /**
     * create a new page result
     *
     * @param data       table data list
     * @param totalCount total data count
     * @param <T>        Table
     * @return Page result
     */
    public static <T> PageResult<T> of(List<T> data, Long totalCount) {
        PageResult<T> pageResult = new PageResult<T>();
        pageResult.setData(data);
        pageResult.setTotalCount(totalCount);
        return pageResult;
    }

    /**
     * empty page result
     *
     * @param <T> Table
     * @return Page result
     */
    public static <T> PageResult<T> empty() {
        PageResult<T> pageResult = new PageResult<T>();
        pageResult.setData(Collections.emptyList());
        pageResult.setTotalCount(0L);
        return pageResult;
    }
}
