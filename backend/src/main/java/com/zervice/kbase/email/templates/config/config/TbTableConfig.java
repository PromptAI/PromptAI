package com.zervice.kbase.email.templates.config.config;

import com.zervice.kbase.email.templates.config.base.TbBorderColorSize;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbTableConfig {

    static final TbTableConfig DEFAULT = new TbTableConfig(new TbTableItem("#51545E", "15px", "18px"),
            new TbBorderColorSize("#EAEAEC", "1px"),
            new TbTableHeading("#85878E", "12px"),
            new TbTableTotal("#333333"));

    public static final TbTableConfig newInstance() {
        return new TbTableConfig(DEFAULT);
    }

    private TbTableItem _item;
    private TbBorderColorSize _border;
    private TbTableHeading _heading;
    private TbTableTotal _total;

    public TbTableConfig(TbTableConfig other) {
        this._item = new TbTableItem(other._item);
        this._border = new TbBorderColorSize(other._border);
        this._heading = new TbTableHeading(other._heading);
        this._total = new TbTableTotal(other._total);
    }

    @Data
    @AllArgsConstructor
    public static class TbTableItem {
        private String _color;
        private String _size;
        private String _lineHeight;

        public TbTableItem(TbTableItem other) {
            this._color = other._color;
            this._size = other._size;
            this._lineHeight = other._lineHeight;
        }
    }

    @Data
    @AllArgsConstructor
    public static class TbTableHeading {
        private String _color;
        private String _size;

        public TbTableHeading(TbTableHeading other) {
            this._color = other._color;
            this._size = other._size;
        }
    }

    @Data
    @AllArgsConstructor
    public static class TbTableTotal {
        private String _color;

        public TbTableTotal(TbTableTotal other) {
            this._color = other._color;
        }
    }
}
