package com.wire.bots.swisscom.DAO;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import java.util.UUID;

public interface SignatureDAO {

    @SqlUpdate("INSERT INTO Signature(response_id, request_id, document_id, user_id, signature) " +
            "VALUES (:responseId, :requestId, :documentId, :userId, :signature) ON CONFLICT(response_id) DO NOTHING")
    int insert(@Bind("responseId") UUID responseId,
               @Bind("requestId") UUID requestId,
               @Bind("documentId") UUID documentId,
               @Bind("userId") UUID userId,
               @Bind("signature") byte[] signature);

}
