package com.wire.bots.swisscom.DAO;

import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.swisscom.model.Signer;
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

public interface SignerDAO {

    @SqlQuery("SELECT * FROM Signer WHERE user_id = :userId")
    @RegisterMapper(_Mapper.class)
    Signer getSigner(@Bind("userId") UUID userId);

    @SqlUpdate("INSERT INTO Signer(user_id, name, email) VALUES (:userId, :name, :email) ON CONFLICT (user_id) DO NOTHING")
    int insert(@Bind("userId") UUID userId,
               @Bind("name") String name,
               @Bind("email") String email);

    @SqlUpdate("UPDATE Signer SET phone = :phone WHERE user_id = :userId")
    int update(@Bind("userId") UUID userId,
               @Bind("phone") String phone);

    class _Mapper implements ResultSetMapper<Signer> {
        @Override
        @Nullable
        public Signer map(int i, ResultSet rs, StatementContext statementContext) {
            try {
                Signer signer = new Signer();

                signer.userId = getUuid(rs, "user_id");
                signer.name = rs.getString("name");
                signer.pseudonym = rs.getString("email");
                signer.phone = rs.getString("phone");

                return signer;
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
