package com.zervice.kbase.email.templates.table;

public interface TableCellLink extends TableCell {
    String getText();

    String getLinkUrl();

    default TableCellType getType() {
        return TableCellType.LINK;
    }
}
