package com.zervice.kbase.eventbus;

import lombok.Getter;

/**
 * Base class for all event that shall be posted to EventBus
 */
public class SystemEvent {
    @Getter
    private final String _name;

    @Getter
    private final Object _data;

    /**
     * An event without data
     * @param name
     */
    public SystemEvent(String name) {
        _name = name;
        _data = null;
    }

    /**
     * An event with data ...
     * @param name
     * @param data
     */
    public SystemEvent(String name, Object data) {
        _name = name;
        _data = data;
    }

    public String toString() {
        return "SystemEvent (name=" + _name + ", data=" + (_data == null ? "<null>": _data.toString()) + ")";
    }
}
