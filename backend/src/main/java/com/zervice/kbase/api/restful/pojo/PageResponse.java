package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.utils.PageResult;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class PageResponse<T> {
    @Getter
    @Setter
    private Long _totalElements;
    @Getter
    @Setter
    private List<T> _contents;

    public PageResponse(Long totalElements, List<T> contents) {
        this._totalElements = totalElements;
        this._contents = contents;
    }

    public static <T> PageResponse<T> of(List<T> contents) {
        return new PageResponse<>((long) contents.size(), contents);
    }

    public static <T> PageResponse<T> of(PageResult<T> page) {
        return new PageResponse<>(page.getTotalCount(), page.getData());
    }

    public static <T> PageResponse<T> of(long totalElements, List<T> contents) {
        return new PageResponse<>(totalElements, contents);
    }
}
