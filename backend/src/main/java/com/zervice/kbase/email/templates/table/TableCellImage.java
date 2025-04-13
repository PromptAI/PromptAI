package com.zervice.kbase.email.templates.table;

public interface TableCellImage extends TableCell {

    String getSrc();

    String getAlt();

    String getWidth();

    String getHeight();

    String getTitle();

    String getLinkUrl();

    default TableCellType getType() {
        return TableCellType.IMAGE;
    }
}
