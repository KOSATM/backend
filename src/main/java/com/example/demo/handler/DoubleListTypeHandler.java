package com.example.demo.handler; // 패키지명은 프로젝트 구조에 맞게 조정하세요

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DoubleListTypeHandler extends BaseTypeHandler<List<Double>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Double> parameter, JdbcType jdbcType) throws SQLException {
        // List를 PostgreSQL vector가 인식할 수 있는 문자열 포맷 "[1.0, 2.0, ...]"으로 변환
        // pgvector 등에서는 String으로 전달 후 DB에서 형변환되거나, Types.OTHER로 전달하면 처리됩니다.
        ps.setObject(i, parameter.toString(), Types.OTHER);
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVector(rs.getString(columnName));
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVector(rs.getString(columnIndex));
    }

    @Override
    public List<Double> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVector(cs.getString(columnIndex));
    }

    private List<Double> parseVector(String vectorString) {
        if (vectorString == null || vectorString.isEmpty()) {
            return null;
        }
        // DB에서 가져온 문자열 "[1.0, 2.0]" 에서 대괄호 제거 후 파싱
        String content = vectorString.replace("[", "").replace("]", "");
        if (content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return Arrays.stream(content.split(","))
                    .map(String::trim)
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            // 파싱 실패 시 빈 리스트 반환하거나 예외 처리
            return new ArrayList<>();
        }
    }
}