package com.echocampus.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@MappedTypes({List.class})
@MappedJdbcTypes(JdbcType.OTHER)
public class UuidListJsonbTypeHandler extends BaseTypeHandler<Object> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<List<UUID>> UUID_LIST_TYPE = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(mapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize to JSON", e);
        }
        ps.setObject(i, pgObject);
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parse(json);
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parse(json);
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parse(json);
    }

    private List<UUID> parse(String json) {
        if (json == null) return null;
        try {
            return mapper.readValue(json, UUID_LIST_TYPE);
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
