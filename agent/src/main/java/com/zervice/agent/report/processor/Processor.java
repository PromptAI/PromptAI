package com.zervice.agent.report.processor;

/**
 * This is the base class to report data back to our servers
 *
 * The reporter is responsible for
 * 1. applying common business logics (like labelling data?)
 * 2. data caching, retransmitting, etc.
 * 3. handling interaction with server
 */
public interface Processor {
    void process(String publishedProjectId, Object data) throws Exception;
}
