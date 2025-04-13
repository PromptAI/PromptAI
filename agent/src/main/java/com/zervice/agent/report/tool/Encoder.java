package com.zervice.agent.report.tool;

/**
 * Encode a data to a format that required by the reporter
 */
public interface Encoder<T> {
    T encode(Object input);
}
