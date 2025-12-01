package com.example.demo.common.global.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

public class VectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType)
            throws SQLException {

        // PG vector format → [1,2,3]
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int idx = 0; idx < parameter.length; idx++) {
            sb.append(parameter[idx]);
            if (idx < parameter.length - 1) sb.append(",");
        }
        sb.append(']');

        PGobject vectorObj = new PGobject();
        vectorObj.setType("vector");
        vectorObj.setValue(sb.toString());

        ps.setObject(i, vectorObj);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String val = rs.getString(columnName);
        return parseVector(val);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String val = rs.getString(columnIndex);
        return parseVector(val);
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String val = cs.getString(columnIndex);
        return parseVector(val);
    }

    private float[] parseVector(String value) {
        if (value == null) return null;

        value = value.replace("[", "").replace("]", "");
        String[] parts = value.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i]);
        }
        return result;
    }
}