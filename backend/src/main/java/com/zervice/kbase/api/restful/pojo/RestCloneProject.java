package com.zervice.kbase.api.restful.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestCloneProject {
    @NotBlank(message = "templateProjectId required")
    private String _templateProjectId;
    @NotBlank(message = "name required")
    private String _name;

    private String _locale;

    private String _welcome;

    private String _fallback;

    private String _image;

    private String _description;
}
