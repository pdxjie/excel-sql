<template>
  <div class="workspace-container">
    <a-layout class="layout-container">
      <!-- Sidebar with Database Explorer -->
      <a-layout-sider width="260" class="sidebar" collapsible v-model:collapsed="siderCollapsed">
        <database-explorer 
          @select-table="handleSelectTable"
          @run-query="handleRunQuery"
          @edit-query="handleEditQuery"
        />
      </a-layout-sider>

      <!-- Main Content -->
      <a-layout>
        <!-- Header -->
        <a-layout-header class="header">
          <div class="header-left">
            <menu-unfold-outlined 
              v-if="siderCollapsed" 
              class="trigger" 
              @click="() => (siderCollapsed = false)" 
            />
            <menu-fold-outlined 
              v-else 
              class="trigger" 
              @click="() => (siderCollapsed = true)" 
            />
            <span class="header-title">{{ currentFileName || 'Excel SQL 工作区' }}</span>
          </div>
          <a-menu mode="horizontal" theme="dark" :selectedKeys="['workspace']" class="header-menu">
            <a-menu-item key="home" @click="goHome">
              <home-outlined /> 首页
            </a-menu-item>
            <a-menu-item key="workspace">
              <code-outlined /> 工作区
            </a-menu-item>
            <a-menu-item key="settings" @click="goSettings">
              <setting-outlined /> 设置
            </a-menu-item>
          </a-menu>
        </a-layout-header>

        <!-- Content -->
        <a-layout-content class="content">
          <a-tabs 
            v-model:activeKey="activeTab" 
            type="editable-card"
            class="workspace-tabs"
            @edit="handleTabEdit"
          >
            <!-- Welcome Tab -->
            <a-tab-pane key="welcome" tab="欢迎" :closable="false">
              <div class="welcome-container">
                <div class="welcome-content">
                  <img :src="logoUrl" alt="Excel SQL Logo" class="welcome-logo" />
                  <h1 class="welcome-title">欢迎使用 Excel SQL Desktop</h1>
                  <p class="welcome-desc">通过SQL查询分析Excel数据，提升您的数据处理效率</p>
                  
                  <div class="feature-list">
                    <div class="feature-item">
                      <file-excel-outlined class="feature-icon" />
                      <div class="feature-text">
                        <h3>Excel文件查询</h3>
                        <p>直接使用SQL查询Excel文件，无需导入数据库</p>
                      </div>
                    </div>
                    <div class="feature-item">
                      <code-outlined class="feature-icon" />
                      <div class="feature-text">
                        <h3>SQL编辑器</h3>
                        <p>强大的SQL编辑器，支持语法高亮和自动完成</p>
                      </div>
                    </div>
                    <div class="feature-item">
                      <table-outlined class="feature-icon" />
                      <div class="feature-text">
                        <h3>数据预览</h3>
                        <p>查询结果以表格形式展示，支持全量数据显示</p>
                      </div>
                    </div>
                  </div>
                  
                  <div class="quick-actions">
                    <a-button type="primary" size="large" @click="openExcelFile">
                      <template #icon><file-excel-outlined /></template>
                      打开 Excel 文件
                    </a-button>
                  </div>
                </div>
              </div>
            </a-tab-pane>
            
            <!-- Query Editor Tabs -->
            <a-tab-pane 
              v-for="tab in editorTabs" 
              :key="tab.id" 
              :tab="tab.title"
              :closable="true"
            >
              <div class="tab-content">
                <splitpanes horizontal :push-other-panes="false" class="default-theme">
                  <pane :size="60" min-size="20">
                    <div class="query-editor-container">
                      <query-editor
                        :database-id="tab.databaseId"
                        :database-name="tab.databaseName"
                        :initial-query="{
                          id: tab.queryId || 'new',
                          name: tab.title,
                          sql: tab.sql || ''
                        }"
                        @run-query="handleRunQuery"
                        @save-query="saveQuery"
                      />
                    </div>
                  </pane>
                  <pane min-size="20">
                    <div class="results-container">
                      <a-tabs v-model:activeKey="activeResultTab" class="result-tabs">
                        <a-tab-pane key="results" tab="查询结果">
                          <div class="table-container">
                            <a-spin :spinning="tableLoading">
                              <div class="table-wrapper">
                                <a-table
                                  :columns="resultColumns"
                                  :data-source="resultData"
                                  :pagination="false"
                                  size="small"
                                  :scroll="{ y: 'calc(100vh - 300px)', x: true }"
                                  class="result-table"
                                  :bordered="true"
                                />
                              </div>
                            </a-spin>
                          </div>
                        </a-tab-pane>
                        
                        <a-tab-pane key="messages" tab="消息">
                          <div class="messages">
                            <div 
                              v-for="(msg, index) in messages" 
                              :key="index"
                              class="message"
                              :class="msg?.type || 'info'"
                            >
                              <check-circle-outlined v-if="msg?.type === 'success'" />
                              <close-circle-outlined v-else-if="msg?.type === 'error'" />
                              <info-circle-outlined v-else-if="msg?.type === 'info' || !msg?.type" />
                              <warning-outlined v-else-if="msg?.type === 'warning'" />
                              {{ msg?.text || '未知消息' }}
                            </div>
                          </div>
                        </a-tab-pane>
                      </a-tabs>
                    </div>
                  </pane>
                </splitpanes>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, onBeforeUnmount, computed, nextTick, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { 
  HomeOutlined, 
  CodeOutlined, 
  SettingOutlined, 
  PlayCircleOutlined, 
  AlignLeftOutlined, 
  SaveOutlined,
  FileExcelOutlined,
  FolderOutlined,
  FileAddOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  TableOutlined
} from '@ant-design/icons-vue';
import DatabaseExplorer from '../components/DatabaseExplorer.vue';
import QueryEditor from '../components/QueryEditor.vue';
import logoUrl from '../assets/logo.png';
import { Splitpanes, Pane } from 'splitpanes';
import 'splitpanes/dist/splitpanes.css';
import { useSqlStore } from '../stores';

export default defineComponent({
  name: 'WorkspaceView',
  components: {
    HomeOutlined,
    CodeOutlined,
    SettingOutlined,
    PlayCircleOutlined,
    AlignLeftOutlined,
    SaveOutlined,
    FileExcelOutlined,
    FolderOutlined,
    FileAddOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    InfoCircleOutlined,
    WarningOutlined,
    TableOutlined,
    DatabaseExplorer,
    QueryEditor,
    Splitpanes,
    Pane
  },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const sqlStore = useSqlStore();
    
    // 状态
    const siderCollapsed = ref(false);
    const resultColumns = ref([]);
    const resultData = ref([]);
    const activeResultTab = ref('results');
    const tableLoading = ref(false);
    const messages = ref([
      { text: '欢迎使用 Excel SQL Desktop', type: 'info' }
    ]);
    
    // 当前文件状态
    const currentFile = computed(() => sqlStore.currentFile);
    const currentFileName = computed(() => {
      return currentFile.value ? currentFile.value.name : '';
    });
    
    // 标签页管理
    const activeTab = ref('welcome');
    const editorTabs = ref([]);
    
    // 解决闪白问题
    const preventFlash = () => {
      // 确保应用了正确的主题
      const theme = localStorage.getItem('theme') || 'light';
      if (theme === 'dark') {
        document.documentElement.classList.add('dark-theme');
        document.documentElement.style.backgroundColor = '#121212';
        document.body.style.backgroundColor = '#121212';
      } else if (theme === 'light') {
        document.documentElement.classList.remove('dark-theme');
        document.documentElement.style.backgroundColor = '#f8f9fa';
        document.body.style.backgroundColor = '#f8f9fa';
      } else if (theme === 'system') {
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (prefersDark) {
          document.documentElement.classList.add('dark-theme');
          document.documentElement.style.backgroundColor = '#121212';
          document.body.style.backgroundColor = '#121212';
        } else {
          document.documentElement.classList.remove('dark-theme');
          document.documentElement.style.backgroundColor = '#f8f9fa';
          document.body.style.backgroundColor = '#f8f9fa';
        }
      }
    };
    
    // 打开 Excel 文件
    const openExcelFile = async () => {
      try {
        const filePath = await window.electronAPI.openFile();
        if (filePath) {
          // 文件已选择，刷新文件列表
          await sqlStore.fetchFiles();
          
          // 找到新添加的文件
          const fileName = filePath.split(/[\\/]/).pop().replace(/\.[^/.]+$/, "");
          const file = sqlStore.excelFiles.find(f => f.name === fileName);
          
          if (file) {
            // 设置当前文件
            sqlStore.setCurrentFile(file.name);
            message.success('文件已打开');
          } else {
            message.warning('文件已选择但未在列表中找到，请刷新');
          }
        }
      } catch (error) {
        console.error('打开文件失败:', error);
        message.error('打开文件失败');
      }
    };
    
    // 处理选择表格
    const handleSelectTable = async (tableInfo) => {
      const { databaseId, tableName } = tableInfo;
      
      // 查找数据库信息
      const database = await findDatabaseById(databaseId);
      if (!database) {
        message.error('未找到数据库');
        return;
      }
      
      // 创建查看表格的查询
      const tabId = `table-${databaseId}-${tableName}`;
      const sql = `SELECT * FROM ${tableName}`;
      
      // 检查标签页是否已存在
      const existingTabIndex = editorTabs.value.findIndex(tab => tab.id === tabId);
      if (existingTabIndex !== -1) {
        // 如果标签页已存在，切换到该标签页
        activeTab.value = tabId;
        return;
      }
      
      // 创建新标签页
      editorTabs.value.push({
        id: tabId,
        title: `${tableName} (${database.name})`,
        type: 'table',
        databaseId,
        databaseName: database.name,
        tableName,
        sql
      });
      
      // 切换到新标签页
      activeTab.value = tabId;
      
      // 执行查询
      executeQuery({
        databaseId,
        sql,
        name: tableName
      });
    };
    
    // 处理运行查询
    const handleRunQuery = (queryInfo) => {
      executeQuery(queryInfo);
    };
    
    // 处理编辑查询
    const handleEditQuery = async (queryInfo) => {
      const { databaseId, queryId, name, sql } = queryInfo;
      
      // 查找数据库信息
      const database = await findDatabaseById(databaseId);
      if (!database) {
        message.error('未找到数据库');
        return;
      }
      
      // 生成标签页ID
      const tabId = `query-${databaseId}-${queryId}`;
      
      // 检查标签页是否已存在
      const existingTabIndex = editorTabs.value.findIndex(tab => tab.id === tabId);
      if (existingTabIndex !== -1) {
        // 如果标签页已存在，切换到该标签页
        activeTab.value = tabId;
        return;
      }
      
      // 创建新标签页
      editorTabs.value.push({
        id: tabId,
        title: name || `新查询 (${database.name})`,
        type: 'query',
        databaseId,
        databaseName: database.name,
        queryId,
        sql: sql || ''
      });
      
      // 切换到新标签页
      activeTab.value = tabId;
    };
    
    // 执行查询
    const executeQuery = async (queryInfo) => {
      const { databaseId, sql, name } = queryInfo;
      if (!sql) {
        message.warning('SQL 查询为空');
        return;
      }
      
      tableLoading.value = true;
      messages.value.push({ text: `正在执行查询: ${sql}`, type: 'info' });
      
      try {
        // 调用API执行查询
        const result = await sqlStore.executeQuery({
          workbook: databaseId,
          sql
        });
        
        // 处理结果
        if (!result.success) {
          throw new Error(result.errorMessage || '查询执行失败');
        }
        
        // 根据 SQL 类型处理不同的响应
        if (result.sqlType === 'SELECT') {
          // 处理 SELECT 查询结果
          if (result.columns && result.rows) {
            // 转换列定义为表格列
            const columns = result.columns.map(col => ({
              title: col.label || col.name,
              dataIndex: col.name,
              key: col.name
            }));
            
            // 为每行数据添加key属性
            const rows = result.rows.map((row, index) => ({
              key: index.toString(),
              ...row
            }));
            
            resultColumns.value = columns;
            resultData.value = rows;
            
            messages.value.push({ 
              text: `查询成功: 返回 ${rows.length} 条记录${result.executionTime ? `, 耗时 ${result.executionTime}ms` : ''}`, 
              type: 'success' 
            });
          } else {
            resultColumns.value = [];
            resultData.value = [];
            messages.value.push({ 
              text: `查询成功，但未返回数据${result.executionTime ? `, 耗时 ${result.executionTime}ms` : ''}`, 
              type: 'info' 
            });
          }
        } else {
          // 处理非 SELECT 查询结果 (CREATE, INSERT, UPDATE, DELETE, USE 等)
          resultColumns.value = [];
          resultData.value = [];
          
          let actionText = '执行';
          switch (result.sqlType) {
            case 'CREATE_WORKBOOK':
              actionText = '创建工作簿';
              break;
            case 'USE_WORKBOOK':
              actionText = '使用工作簿';
              break;
            case 'INSERT':
              actionText = '插入数据';
              break;
            case 'UPDATE':
              actionText = '更新数据';
              break;
            case 'DELETE':
              actionText = '删除数据';
              break;
            case 'DROP':
              actionText = '删除对象';
              break;
            default:
              actionText = '执行';
          }
          
          messages.value.push({ 
            text: `${actionText}成功${result.affectedRows ? `, 影响了 ${result.affectedRows} 行` : ''}${result.executionTime ? `, 耗时 ${result.executionTime}ms` : ''}`, 
            type: 'success' 
          });
        }
        
        tableLoading.value = false;
        activeResultTab.value = 'results';
      } catch (error) {
        tableLoading.value = false;
        resultColumns.value = [];
        resultData.value = [];
        messages.value.push({ text: `查询失败: ${error.message}`, type: 'error' });
        console.error('查询执行失败:', error);
      }
    };
    
    // 辅助函数：根据ID查找数据库
    const findDatabaseById = async (databaseId) => {
      try {
        // 先尝试从store中获取
        await sqlStore.fetchFiles();
        const file = sqlStore.excelFiles.find(file => file.name === databaseId);
        
        if (file) {
          return {
            id: file.name,
            name: file.name,
            path: file.filePath
          };
        }
        return null;
      } catch (error) {
        console.error('获取数据库信息失败:', error);
        return null;
      }
    };
    
    // 保存查询
    const saveQuery = async (queryInfo) => {
      const { databaseId, queryId, name, sql, description } = queryInfo;
      
      try {
        // 这里应该调用API保存查询
        // 假设API返回保存后的查询ID
        message.success(`查询 "${name}" 已保存`);
        
        // 更新标签页标题
        const tabId = `query-${databaseId}-${queryId}`;
        const tabIndex = editorTabs.value.findIndex(tab => tab.id === tabId);
        if (tabIndex !== -1) {
          editorTabs.value[tabIndex].title = name;
        }
      } catch (error) {
        console.error('保存查询失败:', error);
        message.error('保存查询失败');
      }
    };
    
    // 处理标签页编辑（关闭）
    const handleTabEdit = (targetKey, action) => {
      if (action === 'remove') {
        editorTabs.value = editorTabs.value.filter(tab => tab.id !== targetKey);
        if (editorTabs.value.length === 0) {
          activeTab.value = 'welcome';
        } else {
          activeTab.value = editorTabs.value[0].id;
        }
      }
    };
    
    // 返回首页
    const goHome = () => {
      router.push('/');
    };
    
    // 转到设置页面
    const goSettings = () => {
      router.push('/settings');
    };
    
    // 监听路由变化，防止闪白
    watch(() => router.currentRoute.value.path, () => {
      preventFlash();
    });
    
    // 组件挂载时
    onMounted(() => {
      preventFlash();
      
      // 加载Excel文件
      sqlStore.fetchFiles();
      
      // 监听主题变化
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', preventFlash);
    });
    
    // 组件卸载时
    onBeforeUnmount(() => {
      window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', preventFlash);
    });
    
    return {
      logoUrl,
      siderCollapsed,
      currentFile,
      currentFileName,
      resultColumns,
      resultData,
      activeResultTab,
      tableLoading,
      messages,
      activeTab,
      editorTabs,
      openExcelFile,
      handleSelectTable,
      handleRunQuery,
      handleEditQuery,
      executeQuery,
      saveQuery,
      handleTabEdit,
      goHome,
      goSettings
    };
  }
});
</script>

<style scoped>
.workspace-container {
  height: 100vh;
  display: flex;
  background-color: var(--background-color);
}

.layout-container {
  width: 100%;
  height: 100%;
}

.sidebar {
  height: 100vh;
  overflow: hidden;
  background-color: var(--component-background);
  border-right: 1px solid var(--border-color);
}

.header {
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--primary-color);
  box-shadow: var(--box-shadow-base);
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
  color: #fff;
  margin-right: 12px;
}

.header-title {
  color: #fff;
  font-size: 16px;
  font-weight: 500;
}

.header-menu {
  background: transparent;
  border-bottom: none;
  line-height: 64px;
}

.content {
  padding: 0;
  overflow: hidden;
  height: calc(100vh - 64px);
  background-color: var(--background-color);
}

.workspace-tabs {
  height: 100%;
}

.workspace-tabs :deep(.ant-tabs-content) {
  height: calc(100% - 46px);
  overflow: hidden;
}

.workspace-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  overflow: hidden;
  background-color: var(--background-color);
}

.tab-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.query-editor-container {
  flex: 1;
  min-height: 200px;
  border-bottom: 1px solid var(--border-color);
  overflow: hidden;
  height: 100%;
}

.results-container {
  height: 100%;
  overflow: hidden;
  background-color: var(--component-background);
  border-radius: var(--border-radius-base);
  box-shadow: var(--box-shadow-base);
  padding: 10px;
}

/* 自定义分割面板样式 */
:deep(.splitpanes) {
  height: 100% !important;
}

:deep(.splitpanes__pane) {
  overflow: hidden;
}

:deep(.splitpanes--vertical > .splitpanes__splitter) {
  width: 4px;
  background-color: var(--border-color);
}

:deep(.splitpanes--horizontal > .splitpanes__splitter) {
  height: 4px;
  background-color: var(--border-color);
}

:deep(.splitpanes__splitter:hover) {
  background-color: var(--primary-color) !important;
}

.result-tabs {
  height: 100%;
}

.result-tabs :deep(.ant-tabs-content) {
  height: calc(100% - 46px);
  overflow: auto;
}

.table-container {
  height: 100%;
  overflow: auto;
  position: relative;
}

.table-wrapper {
  height: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
}

.result-table {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.result-table :deep(.ant-spin-nested-loading),
.result-table :deep(.ant-spin-container),
.result-table :deep(.ant-table),
.result-table :deep(.ant-table-container) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.result-table :deep(.ant-table-body) {
  flex: 1;
  overflow-y: auto !important;
  max-height: unset !important;
}

.result-table :deep(.ant-table-cell) {
  text-align: center;
  vertical-align: middle;
}

.result-table :deep(.ant-table-thead > tr > th) {
  text-align: center;
  background-color: var(--primary-color);
  color: white;
}

.result-table :deep(.ant-table-tbody > tr:hover > td) {
  background-color: rgba(24, 144, 255, 0.1);
}

.messages {
  padding: 16px;
  height: 100%;
  overflow: auto;
}

.message {
  padding: 8px 12px;
  margin-bottom: 8px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.message.success {
  background-color: rgba(82, 196, 26, 0.1);
  color: #52c41a;
}

.message.error {
  background-color: rgba(245, 34, 45, 0.1);
  color: #f5222d;
}

.message.info {
  background-color: rgba(24, 144, 255, 0.1);
  color: #1890ff;
}

.message.warning {
  background-color: rgba(250, 173, 20, 0.1);
  color: #faad14;
}

.welcome-container {
  height: 100%;
  overflow-y: auto;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 40px 20px;
  background-color: var(--component-background);
}

.welcome-content {
  text-align: center;
  max-width: 800px;
  padding: 40px;
  background-color: var(--background-color);
  border-radius: 8px;
  box-shadow: var(--box-shadow-base);
}

.welcome-logo {
  width: 120px;
  height: 120px;
  margin-bottom: 24px;
  animation: pulse 2s infinite ease-in-out;
}

.welcome-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 16px;
}

.welcome-desc {
  font-size: 18px;
  color: var(--text-color-secondary);
  margin-bottom: 40px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 40px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  text-align: left;
  padding: 16px;
  border-radius: 8px;
  background-color: rgba(24, 144, 255, 0.05);
  transition: all 0.3s;
}

.feature-item:hover {
  background-color: rgba(24, 144, 255, 0.1);
  transform: translateY(-2px);
}

.feature-icon {
  font-size: 32px;
  color: var(--primary-color);
}

.feature-text h3 {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-color);
  margin-bottom: 8px;
}

.feature-text p {
  font-size: 14px;
  color: var(--text-color-secondary);
  margin: 0;
}

.quick-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

.quick-actions .ant-btn {
  padding: 0 24px;
  height: 48px;
  font-size: 16px;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}
</style> 