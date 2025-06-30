package com.excelsql.engine.index;

import com.excelsql.config.ExcelSQLConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 索引管理
 * @Author: IT 派同学
 * @Date: 2025-06-30-22:42
 */

@Component
public class IndexManager {

    @Autowired
    private ExcelSQLConfig config;

    private final Map<String, BPlusTreeIndex> indexCache = new ConcurrentHashMap<>();

    public void createIndex(String workbookName, String sheetName, String columnName) {
        String indexKey = getIndexKey(workbookName, sheetName, columnName);
        Path indexPath = getIndexPath(indexKey);

        try {
            Files.createDirectories(indexPath.getParent());

            BPlusTreeIndex index = new BPlusTreeIndex();
            // TODO: Load data from sheet and build index

            // Save index to file
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(indexPath.toFile()))) {
                oos.writeObject(index);
            }

            indexCache.put(indexKey, index);

        } catch (IOException e) {
            throw new RuntimeException("Failed to create index", e);
        }
    }

    public List<Integer> queryByIndex(String workbookName, String sheetName, String columnName, Object value) {
        String indexKey = getIndexKey(workbookName, sheetName, columnName);
        BPlusTreeIndex index = getIndex(indexKey);

        if (index != null) {
            return index.search(value);
        }

        return Collections.emptyList();
    }

    public boolean hasIndex(String workbookName, String sheetName, String columnName) {
        String indexKey = getIndexKey(workbookName, sheetName, columnName);
        return indexCache.containsKey(indexKey) || Files.exists(getIndexPath(indexKey));
    }

    public void dropIndex(String workbookName, String sheetName, String columnName) {
        String indexKey = getIndexKey(workbookName, sheetName, columnName);
        Path indexPath = getIndexPath(indexKey);

        try {
            Files.deleteIfExists(indexPath);
            indexCache.remove(indexKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to drop index", e);
        }
    }

    private BPlusTreeIndex getIndex(String indexKey) {
        BPlusTreeIndex index = indexCache.get(indexKey);
        if (index == null) {
            // Try to load from file
            Path indexPath = getIndexPath(indexKey);
            if (Files.exists(indexPath)) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexPath.toFile()))) {
                    index = (BPlusTreeIndex) ois.readObject();
                    indexCache.put(indexKey, index);
                } catch (IOException | ClassNotFoundException e) {
                    // Index file corrupted, ignore
                }
            }
        }
        return index;
    }

    private String getIndexKey(String workbookName, String sheetName, String columnName) {
        return workbookName + ":" + sheetName + ":" + columnName;
    }

    private Path getIndexPath(String indexKey) {
        return Paths.get(config.getStorage().getIndexPath(), indexKey + ".idx");
    }
}
