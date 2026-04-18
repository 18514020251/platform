package com.xcvk.platform.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *  分页结果
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> records;

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        return new PageResult<>(total, pageNum, pageSize, records);
    }
}
