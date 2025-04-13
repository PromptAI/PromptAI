package com.zervice.kbase.email.templates.table;

import lombok.Getter;

@Getter
public class TableCellImageSimple implements TableCellImage {

    final String _src;
    String _alt;
    String _width;
    String _height;
    String _title;
    String _linkUrl;

    public TableCellImageSimple(String src) {
        this._src = src;
    }

    public TableCellImage alt(String alt) {
        this._alt = alt;
        return this;
    }

    public TableCellImage title(String title) {
        this._title = title;
        return this;
    }

    public TableCellImage linkUrl(String linkUrl) {
        this._linkUrl = linkUrl;
        return this;
    }


    /**
     * width in pixel
     */
    public TableCellImage width(int width) {
        this._width = String.valueOf(width);
        return this;
    }

    /**
     * height in pixel
     */
    public TableCellImage height(int height) {
        this._height = String.valueOf(height);
        return this;
    }

    /**
     * width in percentage please add %
     */
    public TableCellImage width(String width) {
        this._width = width;
        return this;
    }

    /**
     * height in percentage please add %
     */
    public TableCellImage height(String height) {
        this._height = height;
        return this;
    }
}
