package com.smartcrew.agent.api.rag.service;

import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;

import java.util.List;
import java.util.Optional;

/**
 * 鐭ヨ瘑搴撴湇鍔℃帴鍙ｃ€?
 */
public interface KnowledgeBaseService {

    /**
     * 鍒涘缓鐭ヨ瘑搴撱€?     *
     * @param knowledgeBase 鐭ヨ瘑搴撳疄浣撱€?     * @return 鍒涘缓缁撴灉銆?     */
    KnowledgeBase create(KnowledgeBase knowledgeBase);

    /**
     * 鏇存柊鐭ヨ瘑搴撱€?     *
     * @param knowledgeBase 鐭ヨ瘑搴撳疄浣撱€?     * @return 鏇存柊缁撴灉銆?     */
    KnowledgeBase update(KnowledgeBase knowledgeBase);

    /**
     * 鎸夌紪鐮佸垹闄ょ煡璇嗗簱銆?     *
     * @param baseCode 鐭ヨ瘑搴撶紪鐮併€?     */
    void delete(String baseCode);

    /**
     * 鎸?ID 鏌ヨ鐭ヨ瘑搴撱€?     *
     * @param id 涓婚敭 ID銆?     * @return 鍖归厤缁撴灉銆?     */
    Optional<KnowledgeBase> findById(Long id);

    /**
     * 鎸夌紪鐮佹煡璇㈢煡璇嗗簱銆?     *
     * @param baseCode 鐭ヨ瘑搴撶紪鐮併€?     * @return 鍖归厤缁撴灉銆?     */
    Optional<KnowledgeBase> findByCode(String baseCode);

    /**
     * 鏌ヨ鍏ㄩ儴鐭ヨ瘑搴撱€?     *
     * @return 鐭ヨ瘑搴撳垪琛ㄣ€?     */
    List<KnowledgeBase> findAll();

    /**
     * 鎸?Agent 缂栫爜鏌ヨ缁戝畾鐨勭煡璇嗗簱銆?     *
     * @param agentCode Agent 缂栫爜銆?     * @return 鐭ヨ瘑搴撳垪琛ㄣ€?     */
    List<KnowledgeBase> findByAgentCode(String agentCode);
}
