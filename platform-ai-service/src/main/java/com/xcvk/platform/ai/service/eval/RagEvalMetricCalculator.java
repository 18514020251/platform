package com.xcvk.platform.ai.service.eval;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RAG 评测指标计算器。
 */
public final class RagEvalMetricCalculator {

    private RagEvalMetricCalculator() {
    }

    public static boolean hitAtK(List<Long> expectedChunkIds, List<Long> retrievedChunkIds, int k) {
        if (expectedChunkIds == null || expectedChunkIds.isEmpty()
                || retrievedChunkIds == null || retrievedChunkIds.isEmpty() || k <= 0) {
            return false;
        }

        Set<Long> expectedSet = new HashSet<>(expectedChunkIds);
        int limit = Math.min(k, retrievedChunkIds.size());
        for (int i = 0; i < limit; i++) {
            if (expectedSet.contains(retrievedChunkIds.get(i))) {
                return true;
            }
        }
        return false;
    }

    public static double reciprocalRank(List<Long> expectedChunkIds, List<Long> retrievedChunkIds) {
        if (expectedChunkIds == null || expectedChunkIds.isEmpty()
                || retrievedChunkIds == null || retrievedChunkIds.isEmpty()) {
            return 0.0D;
        }

        Set<Long> expectedSet = new HashSet<>(expectedChunkIds);
        for (int i = 0; i < retrievedChunkIds.size(); i++) {
            if (expectedSet.contains(retrievedChunkIds.get(i))) {
                return 1.0D / (i + 1);
            }
        }
        return 0.0D;
    }

    public static double contextPrecisionAtK(List<Long> expectedChunkIds, List<Long> retrievedChunkIds, int k) {
        if (expectedChunkIds == null || expectedChunkIds.isEmpty()
                || retrievedChunkIds == null || retrievedChunkIds.isEmpty() || k <= 0) {
            return 0.0D;
        }

        Set<Long> expectedSet = new HashSet<>(expectedChunkIds);
        int limit = Math.min(k, retrievedChunkIds.size());
        if (limit == 0) {
            return 0.0D;
        }

        long relevantCount = retrievedChunkIds.stream()
                .limit(limit)
                .filter(expectedSet::contains)
                .count();

        return relevantCount * 1.0D / limit;
    }
}