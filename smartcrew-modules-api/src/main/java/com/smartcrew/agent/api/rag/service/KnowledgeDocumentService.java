package com.smartcrew.agent.api.rag.service;

import com.smartcrew.agent.api.rag.domain.entity.KnowledgeDocument;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * 鐭ヨ瘑鏂囨。鏈嶅姟鎺ュ彛銆?
 */
public interface KnowledgeDocumentService {

    /**
     * 涓婁紶鐭ヨ瘑鏂囨。銆?     *
     * @param baseId 鐭ヨ瘑搴?ID銆?     * @param originalFilename 鍘熷鏂囦欢鍚嶃€?     * @param inputStream 鏂囦欢杈撳叆娴併€?     * @param fileSize 鏂囦欢澶у皬銆?     * @return 鏂囨。璁板綍銆?     */
    KnowledgeDocument upload(Long baseId, String originalFilename, InputStream inputStream, long fileSize);

    /**
     * 鎸夋枃妗?ID 鎵ц鏂囨。澶勭悊銆?     *
     * @param documentId 鏂囨。 ID銆?     * @return 澶勭悊鍚庣殑鏂囨。璁板綍銆?     */
    KnowledgeDocument processDocument(Long documentId);

    /**
     * 鍒犻櫎鏂囨。鍜屽叧鑱斿垏鐗囥€?     *
     * @param documentId 鏂囨。 ID銆?     */
    void deleteDocument(Long documentId);

    /**
     * 鎸?ID 鏌ヨ鏂囨。銆?     *
     * @param documentId 鏂囨。 ID銆?     * @return 鍖归厤缁撴灉銆?     */
    Optional<KnowledgeDocument> findById(Long documentId);

    /**
     * 鎸夌煡璇嗗簱 ID 鏌ヨ鏂囨。鍒楄〃銆?     *
     * @param baseId 鐭ヨ瘑搴?ID銆?     * @return 鏂囨。鍒楄〃銆?     */
    List<KnowledgeDocument> findByBaseId(Long baseId);

    /**
     * 鏌ヨ寰呭鐞嗘枃妗ｅ垪琛ㄣ€?     *
     * @return 鏂囨。鍒楄〃銆?     */
    List<KnowledgeDocument> findPendingDocuments();
}
