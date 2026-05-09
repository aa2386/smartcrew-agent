package com.smartcrew.agent.api.collaboration.domain.model;

/**
 * 协作步骤类型常量。
 */
public final class AgentCollaborationStepTypes {

    public static final String DISPATCH = "DISPATCH";
    public static final String MEMORY_READ = "MEMORY_READ";
    public static final String DECISION = "DECISION";
    public static final String EXECUTION = "EXECUTION";
    public static final String TOOL_CALL = "TOOL_CALL";
    public static final String MEMORY_WRITE = "MEMORY_WRITE";
    public static final String FINAL_RESPONSE = "FINAL_RESPONSE";

    private AgentCollaborationStepTypes() {
    }
}
