package com.zervice.kbase.email.templates.table;


import com.zervice.kbase.email.templates.styling.Alignment;
import com.zervice.kbase.email.templates.styling.FontStyle;
import com.zervice.kbase.email.templates.styling.FontWeight;
import com.zervice.kbase.email.templates.styling.TextDecoration;
import lombok.Getter;

@Getter
public class ColumnConfig {

    String _width;
    Alignment _alignment;
    int _colspan = 1;

    FontStyle _fontStyle;
    FontWeight _fontWeight;
    TextDecoration _textDecoration;

    /**
     * java.text.DecimalFormat
     */
    String _numberFormat;

    /**
     * width in pixel
     */
    public ColumnConfig width(int width) {
        this._width = String.valueOf(width);
        return this;
    }

    /**
     * width in percentage please add %
     */
    public ColumnConfig width(String width) {
        this._width = width;
        return this;
    }

    public ColumnConfig colspan(int colspan) {
        this._colspan = colspan;
        return this;
    }

    public ColumnConfig alignment(Alignment alignment) {
        this._alignment = alignment;
        return this;
    }

    public ColumnConfig left() {
        this._alignment = Alignment.LEFT;
        return this;
    }

    public ColumnConfig center() {
        this._alignment = Alignment.CENTER;
        return this;
    }

    public ColumnConfig right() {
        this._alignment = Alignment.RIGHT;
        return this;
    }

    public ColumnConfig fontStyle(FontStyle fontStyle) {
        this._fontStyle = fontStyle;
        return this;
    }

    public ColumnConfig italic() {
        this._fontStyle = FontStyle.ITALIC;
        return this;
    }

    public ColumnConfig fontWeight(FontWeight fontWeight) {
        this._fontWeight = fontWeight;
        return this;
    }

    public ColumnConfig bold() {
        this._fontWeight = FontWeight.BOLD;
        return this;
    }

    public ColumnConfig lighter() {
        this._fontWeight = FontWeight.LIGHTER;
        return this;
    }

    public ColumnConfig textDecoration(TextDecoration textDecoration) {
        this._textDecoration = textDecoration;
        return this;
    }

    public ColumnConfig underline() {
        this._textDecoration = TextDecoration.UNDERLINE;
        return this;
    }

    public ColumnConfig overline() {
        this._textDecoration = TextDecoration.OVERLINE;
        return this;
    }

    public ColumnConfig lineThrough() {
        this._textDecoration = TextDecoration.LINE_THROUGH;
        return this;
    }

    public ColumnConfig numberFormat(String numberFormat) {
        this._numberFormat = numberFormat;
        return this;
    }

}
