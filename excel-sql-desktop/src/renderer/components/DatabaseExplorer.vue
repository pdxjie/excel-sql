<template>
  <div class="database-explorer">
    <div class="explorer-header">
      <div class="header-title">
        <database-outlined />
        <span class="header-text">数据库浏览器</span>
      </div>
      <div class="header-actions">
        <a-tooltip title="添加 Excel 文件">
          <a-button type="text" size="small" @click="addExcelFile">
            <template #icon><plus-outlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="刷新">
          <a-button type="text" size="small" @click="refreshExplorer">
            <template #icon><reload-outlined /></template>
          </a-button>
        </a-tooltip>
      </div>
    </div>
    
    <a-spin :spinning="loading" class="explorer-content">
      <a-empty v-if="databases.length === 0" description="没有打开的 Excel 文件">
        <template #extra>
          <a-button type="primary" @click="addExcelFile">添加 Excel 文件</a-button>
        </template>
      </a-empty>
      
      <a-tree
        v-else
        :tree-data="treeData"
        :selected-keys="selectedKeys"
        :expanded-keys="expandedKeys"
        :auto-expand-parent="autoExpandParent"
        @select="onSelect"
        @expand="onExpand"
        show-icon
        class="database-tree"
      >
        <template #icon="{ key, data }">
          <file-excel-outlined v-if="data.type === 'database'" />
          <folder-outlined v-else-if="data.type === 'tables-group' || data.type === 'queries-group'" />
          <table-outlined v-else-if="data.type === 'table'" />
          <file-text-outlined v-else-if="data.type === 'query'" />
        </template>
        <template #title="{ key, title, data }">
          <div class="tree-node-title">
            <span class="node-title-text">
              <a-spin v-if="data.isLoading" size="small" class="node-loading-icon" />
              {{ title }}
            </span>
            <div class="node-actions">
              <a-dropdown v-if="data.type === 'database'">
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="refresh" @click="refreshDatabase(data)">
                      <reload-outlined /> 刷新
                    </a-menu-item>
                    <a-menu-item key="newQuery" @click="createNewQuery(data)">
                      <file-add-outlined /> 新建查询
                    </a-menu-item>
                    <a-menu-item key="close" @click="closeDatabase(data)">
                      <close-outlined /> 关闭
                    </a-menu-item>
                  </a-menu>
                </template>
                <more-outlined class="action-icon" />
              </a-dropdown>
              
              <a-dropdown v-else-if="data.type === 'table'">
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="preview" @click="previewTable(data)">
                      <eye-outlined /> 预览
                    </a-menu-item>
                    <a-menu-item key="query" @click="queryTable(data)">
                      <code-outlined /> 查询
                    </a-menu-item>
                  </a-menu>
                </template>
                <more-outlined class="action-icon" />
              </a-dropdown>
              
              <a-dropdown v-else-if="data.type === 'query'">
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="run" @click="runQuery(data)">
                      <play-circle-outlined /> 运行
                    </a-menu-item>
                    <a-menu-item key="edit" @click="editQuery(data)">
                      <edit-outlined /> 编辑
                    </a-menu-item>
                    <a-menu-item key="delete" @click="deleteQuery(data)">
                      <delete-outlined /> 删除
                    </a-menu-item>
                  </a-menu>
                </template>
                <more-outlined class="action-icon" />
              </a-dropdown>
            </div>
          </div>
        </template>
      </a-tree>
    </a-spin>
    
    <a-modal v-model:open="newQueryModal.open" title="新建查询" @ok="handleCreateQuery">
      <a-form :model="newQueryModal.form" layout="vertical">
        <a-form-item label="查询名称" name="name">
          <a-input v-model:value="newQueryModal.form.name" placeholder="输入查询名称" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="newQueryModal.form.description" placeholder="输入查询描述（可选）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import {
  DatabaseOutlined,
  FileExcelOutlined,
  TableOutlined,
  FileTextOutlined,
  PlusOutlined,
  ReloadOutlined,
  MoreOutlined,
  CloseOutlined,
  EyeOutlined,
  CodeOutlined,
  PlayCircleOutlined,
  EditOutlined,
  DeleteOutlined,
  FileAddOutlined,
  FolderOutlined
} from '@ant-design/icons-vue';
import { sqlService } from '../services';
import { useSqlStore } from '../stores';

export default defineComponent({
  name: 'DatabaseExplorer',
  components: {
    DatabaseOutlined,
    FileExcelOutlined,
    TableOutlined,
    FileTextOutlined,
    PlusOutlined,
    ReloadOutlined,
    MoreOutlined,
    CloseOutlined,
    EyeOutlined,
    CodeOutlined,
    PlayCircleOutlined,
    EditOutlined,
    DeleteOutlined,
    FileAddOutlined,
    FolderOutlined
  },
  emits: ['select-table', 'run-query', 'edit-query'],
  setup(props, { emit }) {
    const sqlStore = useSqlStore();
    const loading = computed(() => sqlStore.isLoading);
    const excelFiles = computed(() => sqlStore.excelFiles);
    const selectedKeys = ref([]);
    const expandedKeys = ref([]);
    const autoExpandParent = ref(false);
    // 跟踪正在加载的数据库ID
    const loadingDatabases = ref(new Set());
    
    // 新建查询模态框
    const newQueryModal = reactive({
      open: false,
      databaseId: null,
      form: {
        name: '',
        description: ''
      }
    });
    
    // 构建树形数据
    const treeData = computed(() => {
      return excelFiles.value.map(file => {
        const isLoading = loadingDatabases.value.has(file.name);
        return {
          key: `db-${file.name}`,
          title: isLoading ? `${file.name} (加载中...)` : file.name,
          type: 'database',
          id: file.name,
          path: file.filePath,
          isLoading,
          children: [
            {
              key: `db-${file.name}-tables`,
              title: '表',
              type: 'tables-group',
              selectable: false,
              children: file.sheets.map(sheet => ({
                key: `table-${file.name}-${sheet}`,
                title: sheet,
                type: 'table',
                databaseId: file.name,
                tableName: sheet
              }))
            },
            {
              key: `db-${file.name}-queries`,
              title: '查询',
              type: 'queries-group',
              selectable: false,
              children: []  // 暂时没有保存的查询
            }
          ]
        };
      });
    });
    
    // 加载Excel文件列表
    const loadDatabases = async () => {
      try {
        await sqlStore.fetchFiles();
        // 不再默认展开所有数据库节点，让用户手动展开以触发 USE WORKBOOK 命令
        autoExpandParent.value = false;
      } catch (error) {
        console.error('加载Excel文件列表失败:', error);
        message.error('加载Excel文件列表失败');
      }
    };
    
    // 添加 Excel 文件
    const addExcelFile = async () => {
      try {
        const filePath = await window.electronAPI.openFile();
        if (filePath) {
          message.success('已添加 Excel 文件');
          await loadDatabases(); // 重新加载数据库列表
        }
      } catch (error) {
        console.error('添加 Excel 文件失败:', error);
        message.error('添加 Excel 文件失败');
      }
    };
    
    // 刷新浏览器
    const refreshExplorer = () => {
      loadDatabases();
    };
    
    // 刷新数据库
    const refreshDatabase = async (database) => {
      message.info(`刷新数据库: ${database.title}`);
      try {
        // 保存当前展开的节点
        const currentExpandedKeys = [...expandedKeys.value];
        
        await loadDatabases();
        
        // 恢复之前展开的节点
        expandedKeys.value = currentExpandedKeys;
        message.success('数据库已刷新');
      } catch (error) {
        console.error('刷新数据库失败:', error);
        message.error('刷新数据库失败');
      }
    };
    
    // 创建新查询
    const createNewQuery = (database) => {
      newQueryModal.databaseId = database.id;
      newQueryModal.form.name = '';
      newQueryModal.form.description = '';
      newQueryModal.open = true;
    };
    
    // 处理创建查询
    const handleCreateQuery = () => {
      if (!newQueryModal.form.name) {
        message.error('请输入查询名称');
        return;
      }
      
      // 这里应该处理创建查询的逻辑
      message.success('查询已创建');
      newQueryModal.open = false;
      
      // 触发编辑查询事件
      emit('edit-query', {
        databaseId: newQueryModal.databaseId,
        queryId: 'new',
        name: newQueryModal.form.name,
        description: newQueryModal.form.description,
        sql: ''
      });
    };
    
    // 关闭数据库
    const closeDatabase = (database) => {
      message.info(`关闭数据库: ${database.title}`);
      // 实现关闭数据库的逻辑
      // 这里我们不从列表中移除，因为这是从API获取的
    };
    
    // 预览表
    const previewTable = (table) => {
      emit('select-table', {
        databaseId: table.databaseId,
        tableName: table.tableName
      });
    };
    
    // 查询表
    const queryTable = (table) => {
      message.info(`查询表: ${table.tableName}`);
      emit('edit-query', {
        databaseId: table.databaseId,
        queryId: 'new',
        name: `查询 ${table.tableName}`,
        sql: `SELECT * FROM ${table.tableName}`
      });
    };
    
    // 运行查询
    const runQuery = (query) => {
      message.info(`运行查询: ${query.title}`);
      emit('run-query', {
        databaseId: query.databaseId,
        queryId: query.queryId,
        queryData: query.queryData
      });
    };
    
    // 编辑查询
    const editQuery = (query) => {
      message.info(`编辑查询: ${query.title}`);
      emit('edit-query', {
        databaseId: query.databaseId,
        queryId: query.queryId,
        queryData: query.queryData
      });
    };
    
    // 删除查询
    const deleteQuery = (query) => {
      message.info(`删除查询: ${query.title}`);
      // 实现删除查询的逻辑
    };
    
    // 选择节点
    const onSelect = (selectedKeys, info) => {
      if (selectedKeys.length > 0) {
        const node = info.node;
        if (node.type === 'table') {
          previewTable(node);
        } else if (node.type === 'query') {
          editQuery(node);
        }
      }
    };
    
    // 展开/折叠节点
    const onExpand = async (keys, info) => {
      expandedKeys.value = keys;
      autoExpandParent.value = false;
      
      // 检查是否是数据库节点被展开
      if (info.expanded && info.node.type === 'database') {
        try {
          const databaseId = info.node.id;
          // 添加到加载集合
          loadingDatabases.value.add(databaseId);
          
          // 执行 USE WORKBOOK 命令
          await sqlStore.executeQuery({
            sql: `USE WORKBOOK ${databaseId}`
          });
        } catch (error) {
          console.error('加载数据库失败:', error);
          message.error('加载数据库失败');
        } finally {
          // 从加载集合中移除
          loadingDatabases.value.delete(info.node.id);
        }
      }
    };
    
    // 初始化
    loadDatabases();
    
    return {
      loading,
      databases: excelFiles,
      treeData,
      selectedKeys,
      expandedKeys,
      autoExpandParent,
      newQueryModal,
      loadingDatabases,
      loadDatabases,
      addExcelFile,
      refreshExplorer,
      refreshDatabase,
      createNewQuery,
      handleCreateQuery,
      closeDatabase,
      previewTable,
      queryTable,
      runQuery,
      editQuery,
      deleteQuery,
      onSelect,
      onExpand
    };
  }
});
</script>

<style scoped>
.database-explorer {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  background-color: var(--component-background);
  overflow: hidden;
}

.explorer-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  min-height: 48px;
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
  font-weight: 500;
  overflow: hidden;
  white-space: nowrap;
}

.header-text {
  margin-left: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-title .anticon {
  flex-shrink: 0;
  color: var(--primary-color);
}

.header-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.explorer-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  min-height: 0; /* 确保滚动正常工作 */
}

.database-tree {
  width: 100%;
}

.tree-node-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0; /* 确保弹性布局正常工作 */
}

.node-title-text {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.node-loading-icon {
  margin-right: 4px;
  flex-shrink: 0;
}

.node-actions {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
  margin-left: 4px;
}

.tree-node-title:hover .node-actions {
  opacity: 1;
}

.action-icon {
  cursor: pointer;
  color: var(--text-color-secondary);
}

.action-icon:hover {
  color: var(--primary-color);
}

:deep(.ant-tree-treenode) {
  width: 100%;
  display: flex;
  align-items: center;
  padding-right: 4px !important;
}

:deep(.ant-tree-node-content-wrapper) {
  flex: 1;
  overflow: hidden;
  display: flex;
  align-items: center;
}

:deep(.ant-tree-title) {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

:deep(.ant-tree-iconEle) {
  flex-shrink: 0;
  margin-right: 4px !important;
}

/* 确保在收缩侧边栏时图标正确对齐 */
:deep(.ant-tree-indent) {
  flex-shrink: 0;
}

:deep(.ant-tree-switcher) {
  flex-shrink: 0;
}
</style> 