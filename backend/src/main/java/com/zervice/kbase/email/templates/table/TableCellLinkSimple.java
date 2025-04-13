package com.zervice.kbase.email.templates.table;

import lombok.Getter;

@Getter
public class TableCellLinkSimple implements TableCellLink {

    final String _text;
    final String _linkUrl;

    public TableCellLinkSimple(String text, String linkUrl) {
        this._text = text;
        this._linkUrl = linkUrl;
    }
}
