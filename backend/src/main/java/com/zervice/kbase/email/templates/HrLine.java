package com.zervice.kbase.email.templates;

import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class HrLine implements TemplateLine {

    String _margin;

    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    HrLine(EmailTemplateBuilder.EmailTemplateConfigBuilder builder) {
        this._builder = builder;
    }

    public HrLine margin(String margin) {
        this._margin = margin;
        return this;
    }

    @Override
    public TemplateLineType getType() {
        return TemplateLineType.HR;
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
