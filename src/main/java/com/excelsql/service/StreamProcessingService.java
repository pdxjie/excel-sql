package com.excelsql.service;

import com.excelsql.util.MemoryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @Description: 流式处理服务
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:44
 */

@Service
public class StreamProcessingService {

    @Resource
    private MemoryManager memoryManager;

    public Stream<Map<String, Object>> streamLargeDataset(String workbookName, String sheetName) {
        // TODO: Implement streaming for large datasets
        // This would use Apache POI's streaming reader for large files
        return Stream.empty();
    }

    public boolean shouldUseStreaming(long estimatedDataSize) {
        return estimatedDataSize > 10 * 1024 * 1024 || // 10MB threshold
                memoryManager.isMemoryPressure();
    }
}
