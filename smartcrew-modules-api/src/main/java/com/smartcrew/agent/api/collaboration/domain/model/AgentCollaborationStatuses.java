package com.smartcrew.agent.api.collaboration.domain.model;

/**
 * 协作日志状态常量。
 */
public final class AgentCollaborationStatuses {

    /** 等待中 */
    public static final String PENDING = "PENDING";
    /** 执行中 */
    public static final String RUNNING = "RUNNING";
    /** 执行成功 */
    public static final String SUCCESS = "SUCCESS";
    /** 执行失败 */
    public static final String FAILED = "FAILED";
    /** 已跳过 */
    public static final String SKIPPED = "SKIPPED";

    private AgentCollaborationStatuses() {
    }
}
