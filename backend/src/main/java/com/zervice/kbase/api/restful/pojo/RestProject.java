package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.api.restful.pojo.mica.Button;
import com.zervice.kbase.database.pojo.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * @author Peng Chen
 * @date 2022/6/8
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestProject {

    public RestProject(Project project, RestPublishedProject debug, RestPublishedProject published) {
        this(project);
        this.setDebugProject(debug);
        this.setPublishedProject(published);
    }

    /**
     * id
     */
    private String _id;

    @NotBlank
    @Size(min = 1, max = 32, message = "项目名称必须在1-32个字符之间")
    private String _name;

    @NotBlank
    private String _locale;

    private String _image;

    private String _description;

    private Long _createTime;

    private Long _updateTime;

    private Project.Prop _properties;

    private RestPublishedProject _publishedProject;

    private RestPublishedProject _debugProject;

    private String _welcome;

    private String _fallback;

    /**
     * 跟着fallback一起出来的按钮
     */
    private List<Button> _fallbackButtons;

    @Builder.Default
    private String _fallbackType = Project.Prop.FALLBACK_TYPE_TEXT;

    /**
     * 默认回答
     * 这支持一个webhook
     */
    private List<RestProjectComponentBot.BotResponse> _webhooks;

    /**
     * 是否选择展示节点信息（conversations）
     */
    private String _showSubNodesAsOptional;

    /**
     * {@link #_showSubNodesAsOptional} 如果为 {@link Project.Prop#SHOW_SUB_NODES_AS_OPTIONAL_CUSTOM}
     * 这里面存的需要展示的ids, 需要在project里面配置
     * <p>
     * 如果是null 不处理
     */
    private Set<String> _showShbNodesAsOptionalIds;

    private Project.ChatBotSetting _chatBotSettings;

    /**
     * a1库所用到的介绍信息 - markdown
     */
    private String _introduction;

    /**
     * a1 是否可被克隆
     */
    private Boolean _viewable;

    /**
     * llm chat 对话调度模式
     */
    private String _schedule;


    public RestProject(Project project) {
        this._id = project.getId();
        this._name = project.getName();
        this._locale = project.getProperties().getLocale();
        this._image = project.getProperties().getImage();
        this._description = project.getProperties().getDescription();
        this._createTime = project.getProperties().getCreateTime();
        this._updateTime = project.getProperties().getUpdateTime();
        this._properties = project.getProperties();
        if (StringUtils.isNotBlank(project.getProperties().getWelcome())) {
            this._welcome = project.getProperties().getWelcome().trim();
        }
        if (StringUtils.isNotBlank(project.getProperties().getFallback())) {
            this._fallback = project.getProperties().getFallback().trim();
        }
        this._fallbackButtons = project.getProperties().getFallbackButtons();
        this._fallbackType = project.getProperties().getFallbackType();
        this._webhooks = project.getProperties().getWebhooks();

        this._showSubNodesAsOptional = project.getProperties().getShowSubNodesAsOptional();
        this._chatBotSettings = project.getProperties().getChatBotSettings();

        this._introduction = project.getProperties().getIntroduction();
        this._viewable = project.getProperties().getViewable();
        this._schedule = project.getProperties().getSchedule();
    }
}
