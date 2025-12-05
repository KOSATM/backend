package com.example.demo.supporter.checklist.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.demo.supporter.checklist.dto.entity.Checklist;
import java.util.List;

/**
 * Checklist 테이블 데이터 접근 객체
 */
@Mapper
public interface ChecklistDao {
    
    /**
     * 새로운 체크리스트 생성
     */
    int insertChecklist(Checklist checklist);
    
    /**
     * 체크리스트 ID로 조회
     */
    Checklist selectChecklistById(@Param("id") Long id);
    
    /**
     * 사용자 ID와 dayIndex로 체크리스트 조회
     */
    Checklist selectChecklistByUserIdAndDayIndex(
        @Param("userId") Long userId,
        @Param("dayIndex") Integer dayIndex
    );
    
    /**
     * 사용자의 모든 체크리스트 조회
     */
    List<Checklist> selectChecklistsByUserId(@Param("userId") Long userId);
    
    /**
     * 체크리스트 수정
     */
    int updateChecklist(Checklist checklist);
    
    /**
     * 체크리스트 삭제
     */
    int deleteChecklist(@Param("id") Long id);
}