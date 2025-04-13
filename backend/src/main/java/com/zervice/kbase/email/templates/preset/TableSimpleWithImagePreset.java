package com.zervice.kbase.email.templates.preset;

import com.zervice.kbase.email.templates.EmailTemplateBuilder;
import com.zervice.kbase.email.templates.TableLine;
import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import com.zervice.kbase.email.templates.styling.Alignment;
import com.zervice.kbase.email.templates.table.ColumnConfig;
import com.zervice.kbase.email.templates.table.TableCellImageSimple;
import lombok.AccessLevel;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class TableSimpleWithImagePreset implements TableLine {

    final String _numberFormat;

    List<List<Object>> _headerRows = new ArrayList<>();
    List<List<Object>> _itemRows = new ArrayList<>();
    List<List<Object>> _footerRows = new ArrayList<>();

    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    public TableSimpleWithImagePreset(EmailTemplateBuilder.EmailTemplateConfigBuilder builder, String numberFormat) {
        this._builder = builder;
        this._numberFormat = numberFormat;
    }

    @Override
    public EmailTemplateBuilder.EmailTemplateConfigBuilder and() {
        return _builder;
    }

    @Override
    public HtmlTextEmail build() {
        return _builder.build();
    }

    public TableSimpleWithImagePreset headerRow(String imageName, String descriptionName, String amountName) {
        _headerRows.add(Arrays.asList(imageName, descriptionName, amountName));
        return this;
    }

    public TableSimpleWithImagePreset itemRow(String imageUrl, String description, BigDecimal amount) {
        _itemRows.add(Arrays.asList(new TableCellImageSimple(imageUrl), description, amount));
        return this;
    }

    public TableSimpleWithImagePreset footerRow(String totalName, BigDecimal amount) {
        _footerRows.add(Arrays.asList(totalName, amount));
        return this;
    }

    @Override
    public List<ColumnConfig> getHeader() {
        return Arrays.asList(new ColumnConfig()
                        .width("20%"),
                new ColumnConfig()
                        .width("60%"),
                new ColumnConfig()
                        .alignment(Alignment.RIGHT)
                        .width("20%"));
    }

    @Override
    public List<ColumnConfig> getItem() {
        return Arrays.asList(new ColumnConfig(),
                new ColumnConfig(),
                new ColumnConfig()
                        .alignment(Alignment.RIGHT)
                        .numberFormat(_numberFormat));
    }

    @Override
    public List<ColumnConfig> getFooter() {
        return Arrays.asList(new ColumnConfig()
                        .colspan(2)
                        .alignment(Alignment.RIGHT),
                new ColumnConfig()
                        .alignment(Alignment.RIGHT)
                        .numberFormat(_numberFormat));
    }
}
