package com.zervice.kbase.api.restful.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.annotation.Nullable;

@Getter
//@Setter
public class PageRequest {

    public static final String SORT_ID = "id";
    public static final String SORT_CREATE_TIME = "createTime";

    private Integer _page = 0; // 当前page
    // -1 获取所有
    @Setter
    private Integer _size = -1; // 当前page的size

    private boolean _getAll;

    private String _sort;

    public static PageRequest pageAll() {
        return new PageRequest(0, -1, null);
    }
    public PageRequest(Integer page, Integer size, String sort) {
        if (page == null) {
            _getAll = true;
        } else {
            if (size == null) {
                throw new IllegalArgumentException("No size found in request but page is set " + page);
            }

            if (size == -1) {
                _getAll = true;
            } else if (size <= 0) {
                throw new IllegalArgumentException("Invalid size found in request - " + size);
            } else {
                this._page = page;
                this._size = size;
            }
        }
        this._sort = sort;
    }

    public boolean isGetAll() {
        return _size == -1;
    }

    public boolean hasSort() {
        return !StringUtils.isEmpty(_sort) && parse2Sort(_sort) != null;
    }

    public @Nullable SortParam getSortParam() {
        return parse2Sort(_sort);
    }

    public  Sort getSort() {
        SortParam sortParam = getSortParam();
        if (sortParam == null) {
            return Sort.unsorted();
        }

        return  Sort.by(sortParam.isAsc() ? Sort.Direction.ASC : Sort.Direction.DESC, sortParam.getSortBy());
    }


    @Getter
    @AllArgsConstructor
    public static class SortParam {
        String _sortBy;
        boolean _asc;
    }

    public static SortParam parse2Sort(String s) {
        if (s == null) {
            return null;
        } else {
            String [] fieldAndDirection = s.split(",");
            if (fieldAndDirection.length != 2) {
                return null;
            }
            return new SortParam(fieldAndDirection[0], fieldAndDirection[1].equalsIgnoreCase("asc"));
        }
    }

    public static void buildSortAndLimitSql(PageRequest pageRequest, StringBuilder pageSql) {
        buildSortAndLimitSql(pageRequest, pageSql, null);
    }

    /**
     * append sort by and limit
     * @param pageRequest
     * @param pageSql
     * @param tableName  if it's null, we won't append the table name with the sort by field
     */
    public static void buildSortAndLimitSql(PageRequest pageRequest, StringBuilder pageSql, String tableName) {
        int page = pageRequest.getPage();
        int size = pageRequest.getSize();
        PageRequest.SortParam sortParam = pageRequest.getSortParam();
        if (sortParam != null) {
            pageSql.append(" order by ").append(tableName == null ? "" : tableName + ".").append(sortParam.getSortBy()).append(sortParam.isAsc() ? " asc " : " desc ");
        }

        if (!pageRequest.isGetAll()) {
            pageSql.append(" limit ").append(page * size).append(",").append(size);
        }
    }

    public  Pageable toMongoPageable(){
        int page = getPage();
        int size = getSize();
        Sort sort = getSort();
        return org.springframework.data.domain.PageRequest.of(page, size, sort);
    }
}
