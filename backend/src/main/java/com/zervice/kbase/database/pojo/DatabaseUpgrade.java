package com.zervice.kbase.database.pojo;

import lombok.Getter;
import lombok.Setter;

public class DatabaseUpgrade {
    @Getter @Setter
    long _id;

    @Getter @Setter
    int _ver;

    @Getter @Setter
    String _ticket;

    @Getter @Setter
    long _executedEpochMs;
}
