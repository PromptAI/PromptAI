package com.zervice.kbase.email.templates.preset;

import com.zervice.kbase.email.templates.EmailTemplateBuilder;
import com.zervice.kbase.email.templates.TableLine;
import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import com.zervice.kbase.email.templates.styling.Alignment;
import com.zervice.kbase.email.templates.table.ColumnConfig;
import lombok.AccessLevel;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class TableFourColumnPreset implements TableLine {

    final String _taxFormat;
    final String _amountFormat;

    List<List<Object>> _headerRows = new ArrayList<>();
    List<List<Object>> _itemRows = new ArrayList<>();
    List<List<Object>> _footerRows = new ArrayList<>();

    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    public TableFourColumnPreset(EmailTemplateBuilder.EmailTemplateConfigBuilder builder, String taxFormat, String amountFormat) {
        this._builder = builder;
        this._taxFormat = taxFormat;
        this._amountFormat = amountFormat;
    }

    @Override
    public EmailTemplateBuilder.EmailTemplateConfigBuilder and() {
        return _builder;
    }

    @Override
    public HtmlTextEmail build() {
        return _builder.build();
    }

    public TableFourColumnPreset headerRow(String quantityName, String descriptionName, String taxName, String amountName) {
        _headerRows.add(Arrays.asList(quantityName, descriptionName, taxName, amountName));
        return this;
    }

    public TableFourColumnPreset itemRow(Integer quantity, String description, BigDecimal tax, BigDecimal amount) {
        _itemRows.add(Arrays.asList(quantity, description, tax, amount));
        return this;
    }

    public TableFourColumnPreset footerRow(String label, BigDecimal amount) {
        _footerRows.add(Arrays.asList(label, amount));
        return this;
    }

    @Override
    public List<ColumnConfig> getHeader() {
        return Arrays.asList(new ColumnConfig(),
                new ColumnConfig()
                        .width("60%"),
                new ColumnConfig()
                        .alignment(Alignment.RIGHT),
                new ColumnConfig()
                        .width("20%")
                        .alignment(Alignment.RIGHT));
    }

    @Override
    public List<ColumnConfig> getItem() {
        return Arrays.asList(new ColumnConfig(),
                new ColumnConfig(),
                new ColumnConfig()
                        .alignment(Alignment.RIGHT)
                        .numberFormat(_taxFormat),
                new ColumnConfig()
                        .alignment(Alignment.RIGHT)
                        .numberFormat(_amountFormat));
    }

    @Override
    public List<ColumnConfig> getFooter() {
        return Arrays.asList(new ColumnConfig()
                        .colspan(3)
                        .alignment(Alignment.RIGHT),
                new ColumnConfig()
                        .alignment(Alignment.RIGHT)
                        .numberFormat(_amountFormat));
    }
}
