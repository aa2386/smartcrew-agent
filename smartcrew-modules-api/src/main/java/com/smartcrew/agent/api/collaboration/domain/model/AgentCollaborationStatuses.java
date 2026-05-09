package com.smartcrew.agent.api.collaboration.domain.model;

/**
 * 协作日志状态常量。
 */
public final class AgentCollaborationStatuses {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String SKIPPED = "SKIPPED";

    private AgentCollaborationStatuses() {
    }
}
