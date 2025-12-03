package com.example.demo.supporter.checklist.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.demo.supporter.checklist.dto.entity.ChecklistItem;
import java.util.List;

/**
 * ChecklistItem 테이블 데이터 접근 객체
 */
@Mapper
public interface ChecklistItemDao {
    
    /**
     * 새로운 체크리스트 항목 생성
     */
    int insertChecklistItem(ChecklistItem checklistItem);
    
    /**
     * 체크리스트 항목 ID로 조회
     */
    ChecklistItem selectChecklistItemById(@Param("id") Long id);
    
    /**
     * 특정 체크리스트의 모든 항목 조회
     */
    List<ChecklistItem> selectChecklistItemsByChecklistId(@Param("checklistId") Long checklistId);
    
    /**
     * 체크리스트 항목 수정
     */
    int updateChecklistItem(ChecklistItem checklistItem);
    
    /**
     * 체크리스트 항목 삭제
     */
    int deleteChecklistItem(@Param("id") Long id);
    
    /**
     * 특정 체크리스트의 모든 항목 삭제
     */
    int deleteChecklistItemsByChecklistId(@Param("checklistId") Long checklistId);
}