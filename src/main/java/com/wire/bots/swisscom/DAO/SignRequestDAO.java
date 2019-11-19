package com.wire.bots.swisscom.DAO;

import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.swisscom.model.SignRequest;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface SignRequestDAO {

    @SqlQuery("SELECT * FROM SignRequest")
    @RegisterMapper(_Mapper.class)
    List<SignRequest> get();

    @SqlUpdate("INSERT INTO SignRequest(request_id, response_id, bot_id, user_id, document_id) " +
            "VALUES (:requestId, :responseId, :botId, :userId, :documentId)")
    int insert(@Bind("requestId") UUID requestId,
               @Bind("responseId") UUID responseId,
               @Bind("botId") UUID botId,
               @Bind("userId") UUID userId,
               @Bind("documentId") UUID documentId);

    @SqlUpdate("DELETE FROM SignRequest WHERE request_id = :requestId")
    int delete(@Bind("requestId") UUID requestId);

    class _Mapper implements ResultSetMapper<SignRequest> {
        @Override
        @Nullable
        public SignRequest map(int i, ResultSet rs, StatementContext statementContext) {
            try {
                SignRequest signRequest = new SignRequest();

                signRequest.requestId = getUuid(rs, "request_id");
                signRequest.responseId = getUuid(rs, "response_id");
                signRequest.userId = getUuid(rs, "user_id");
                signRequest.botId = getUuid(rs, "bot_id");
                signRequest.documentId = getUuid(rs, "document_id");

                return signRequest;
            } catch (SQLException e) {
                Logger.error("SignerDAOMapper: i: %d, e: %s", i, e);
                return null;
            }
        }

        private UUID getUuid(ResultSet rs, String name) throws SQLException {
            UUID contact = null;
            Object rsObject = rs.getObject(name);
            if (rsObject != null)
                contact = (UUID) rsObject;
            return contact;
        }
    }
}
