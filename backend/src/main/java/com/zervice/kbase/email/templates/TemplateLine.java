package com.zervice.kbase.email.templates;


import com.zervice.kbase.email.templates.model.HtmlTextEmail;

public interface TemplateLine {

    TemplateLineType getType();

    EmailTemplateBuilder.EmailTemplateConfigBuilder and();

    HtmlTextEmail build();

    enum TemplateLineType {
        HR, TEXT, HTML, MARKDOWN, BUTTON, IMAGE, ATTRIBUTE, TABLE, COPYRIGHT;
    }
}
