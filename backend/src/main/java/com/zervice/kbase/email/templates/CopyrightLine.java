package com.zervice.kbase.email.templates;

import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CopyrightLine implements TemplateLine {

    final String _name;
    Integer _year;
    String _url;

    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    CopyrightLine(EmailTemplateBuilder.EmailTemplateConfigBuilder builder, String name) {
        this._builder = builder;
        this._year = LocalDate.now().getYear();
        this._name = name;
    }

    public CopyrightLine year(Integer year) {
        this._year = year;
        return this;
    }

    public CopyrightLine url(String url) {
        this._url = url;
        return this;
    }

    @Override
    public TemplateLineType getType() {
        return TemplateLineType.COPYRIGHT;
    }

    @Override
    public EmailTemplateBuilder.EmailTemplateConfigBuilder and() {
        return _builder;
    }

    @Override
    public HtmlTextEmail build() {
        return _builder.build();
    }
}
