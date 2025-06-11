package com.czdxwx.aiinterviewcoachbackend.config.mybatis;

import com.pgvector.PGvector;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(PGvector.class)
public class PGvectorTypeHandler extends BaseTypeHandler<PGvector> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PGvector parameter, JdbcType jdbcType) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType("vector");
        obj.setValue(parameter.toString());
        ps.setObject(i, obj);
    }

    @Override
    public PGvector getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String str = rs.getString(columnName);
        return str == null ? null : new PGvector(str);
    }

    @Override
    public PGvector getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String str = rs.getString(columnIndex);
        return str == null ? null : new PGvector(str);
    }

    @Override
    public PGvector getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String str = cs.getString(columnIndex);
        return str == null ? null : new PGvector(str);
    }
}