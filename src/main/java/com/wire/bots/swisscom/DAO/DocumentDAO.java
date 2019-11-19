package com.wire.bots.swisscom.DAO;

import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.swisscom.model.Document;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public interface DocumentDAO {

    @SqlUpdate("INSERT INTO Document(document_id, original, owner, name) " +
            "VALUES (:documentId, :original, :owner, :name) ON CONFLICT(document_id) DO NOTHING")
    int insert(@Bind("documentId") UUID documentId,
               @Bind("original") byte[] original,
               @Bind("owner") UUID owner,
               @Bind("name") String name);

    @SqlUpdate("UPDATE Document SET signed = :signed WHERE document_id = :documentId")
    int update(@Bind("documentId") UUID documentId,
               @Bind("signed") byte[] signed);

    @SqlQuery("SELECT original FROM Document WHERE document_id = :documentId")
    byte[] getOriginal(@Bind("documentId") UUID documentId);

    @SqlQuery("SELECT signed FROM Document WHERE document_id = :documentId")
    byte[] getSigned(@Bind("documentId") UUID documentId);

    @SqlQuery("SELECT document_id, owner, name FROM Document WHERE document_id = :documentId")
    @RegisterMapper(_Mapper.class)
    Document getDocument(@Bind("documentId") UUID documentId);

    class _Mapper implements ResultSetMapper<Document> {
        @Override
        @Nullable
        public Document map(int i, ResultSet rs, StatementContext statementContext) {
            try {
                Document document = new Document();
                document.documentId = getUuid(rs, "document_id");
                document.owner = getUuid(rs, "owner");
                document.name = rs.getString("name");

                return document;
            } catch (SQLException e) {
                Logger.error("DocumentDAO: i: %d, e: %s", i, e);
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
