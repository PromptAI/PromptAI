package com.zervice.common.agent;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class AgentClientStatusInfor {

    String _url;

    /**
     * {@link com.zervice.common.agent.AgentClient#_status}  name of the enum
     */
    String _statusName;


    String _accountName;
}
