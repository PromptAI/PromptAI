package com.zervice.kbase.api.restful.pojo.mica;

import lombok.*;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Button {
    private String _id;

    private String _type;

    private String _text;
}