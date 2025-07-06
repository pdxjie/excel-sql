<template>
  <app-layout :show-sider="false" :show-footer="false">
    <div class="home-container">
      <div class="hero-section">
        <div class="hero-content">
          <img :src="logoUrl" alt="Excel SQL Logo" class="logo-image" />
          <h1 class="app-title">Excel SQL Desktop</h1>
          <p class="app-description">假装是一款强大的 Excel 数据分析工具，用 SQL 的方式操作来处理数据</p>
          
          <div class="action-buttons">
            <a-button type="primary" size="large" @click="openExcelFile" class="main-action-btn">
              <template #icon><file-outlined /></template>
              打开 Excel 文件
            </a-button>
            <a-button size="large" @click="createNewWorkspace" class="main-action-btn">
              <template #icon><code-outlined /></template>
              新建工作区
            </a-button>
          </div>
        </div>
      </div>
      
      <div class="features-section">
        <a-row :gutter="[24, 24]">
          <a-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <a-card class="feature-card" hoverable @click="openExcelFile">
              <template #cover>
                <div class="card-icon-container blue-gradient">
                  <file-excel-outlined class="card-icon" />
                </div>
              </template>
              <a-card-meta title="打开 Excel 文件">
                <template #description>
                  <p>打开现有 Excel 文件进行 SQL 查询和数据分析</p>
                  <a-button type="link">立即打开 <right-outlined /></a-button>
                </template>
              </a-card-meta>
            </a-card>
          </a-col>
          
          <a-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <a-card class="feature-card" hoverable @click="createNewWorkspace">
              <template #cover>
                <div class="card-icon-container green-gradient">
                  <code-outlined class="card-icon" />
                </div>
              </template>
              <a-card-meta title="创建工作区">
                <template #description>
                  <p>创建新的工作区，从头开始构建您的数据分析项目</p>
                  <a-button type="link">立即创建 <right-outlined /></a-button>
                </template>
              </a-card-meta>
            </a-card>
          </a-col>
          
          <a-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <a-card class="feature-card" hoverable @click="createNewExcel">
              <template #cover>
                <div class="card-icon-container purple-gradient">
                  <table-outlined class="card-icon" />
                </div>
              </template>
              <a-card-meta title="创建 Excel 文件">
                <template #description>
                  <p>从头创建新的 Excel 文件，定义数据结构和内容</p>
                  <a-button type="link">立即创建 <right-outlined /></a-button>
                </template>
              </a-card-meta>
            </a-card>
          </a-col>
        </a-row>
      </div>
      
<!--      <div class="recent-section">-->
<!--        <h2 class="section-title">最近打开的文件</h2>-->
<!--        <a-spin :spinning="loading">-->
<!--          <a-list -->
<!--            class="recent-list"-->
<!--            :data-source="recentFiles" -->
<!--            :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 4, xxl: 4 }"-->
<!--          >-->
<!--            <template #renderItem="{ item }">-->
<!--              <a-list-item>-->
<!--                <a-card hoverable class="recent-card" @click="openExistingFile(item.path)">-->
<!--                  <template #cover>-->
<!--                    <div class="file-icon-container">-->
<!--                      <file-excel-outlined class="file-icon" />-->
<!--                    </div>-->
<!--                  </template>-->
<!--                  <a-card-meta :title="item.name">-->
<!--                    <template #description>-->
<!--                      <p class="file-path">{{ truncatePath(item.path) }}</p>-->
<!--                      <p class="file-date">{{ formatDate(item.lastOpened) }}</p>-->
<!--                    </template>-->
<!--                  </a-card-meta>-->
<!--                  <template #actions>-->
<!--                    <eye-outlined key="open" @click.stop="openExistingFile(item.path)" />-->
<!--                    <delete-outlined key="delete" @click.stop="removeRecentFile(item)" />-->
<!--                  </template>-->
<!--                </a-card>-->
<!--              </a-list-item>-->
<!--            </template>-->
<!--            <template #empty>-->
<!--              <div class="empty-recent">-->
<!--                <inbox-outlined class="empty-icon" />-->
<!--                <p>没有最近打开的文件</p>-->
<!--                <a-button type="primary" @click="openExcelFile">打开文件</a-button>-->
<!--              </div>-->
<!--            </template>-->
<!--          </a-list>-->
<!--        </a-spin>-->
<!--      </div>-->
      
      <div class="tips-section">
        <a-row :gutter="[24, 24]">
          <a-col :xs="24" :sm="24" :md="12" :lg="8" :xl="8">
            <a-card class="tip-card" hoverable @click="openDocumentation">
              <template #title>
                <book-outlined /> 快速入门
              </template>
              <ul class="tip-list">
                <li>打开 Excel 文件或创建新工作区</li>
                <li>在 SQL 编辑器中编写查询</li>
                <li>执行查询并查看结果</li>
                <li>导出结果为 Excel、CSV 或 JSON</li>
              </ul>
              <a-button type="link" block>查看教程 <right-outlined /></a-button>
            </a-card>
          </a-col>
          
          <a-col :xs="24" :sm="24" :md="12" :lg="8" :xl="8">
            <a-card class="tip-card" hoverable @click="showSqlTips">
              <template #title>
                <code-outlined /> SQL 技巧
              </template>
              <ul class="tip-list">
                <li>使用 SELECT * FROM Sheet1 查询所有数据</li>
                <li>支持 WHERE, GROUP BY, ORDER BY 等标准 SQL</li>
                <li>可以跨表查询和连接多个工作表</li>
                <li>支持聚合函数和高级过滤</li>
              </ul>
              <a-button type="link" block>更多技巧 <right-outlined /></a-button>
            </a-card>
          </a-col>
          
          <a-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
            <a-card class="tip-card system-card" hoverable>
              <template #title>
                <laptop-outlined /> 系统信息
              </template>
              <a-descriptions :column="1" size="small">
                <a-descriptions-item label="应用版本">{{ appInfo.version }}</a-descriptions-item>
                <a-descriptions-item label="Electron 版本">{{ appInfo.electronVersion }}</a-descriptions-item>
                <a-descriptions-item label="Node.js 版本">{{ appInfo.nodeVersion }}</a-descriptions-item>
                <a-descriptions-item label="操作系统">{{ appInfo.platform }}</a-descriptions-item>
              </a-descriptions>
              <a-button type="link" block @click="goSettings">查看设置 <right-outlined /></a-button>
            </a-card>
          </a-col>
        </a-row>
      </div>
      
      <a-back-top />
      
      <a-modal v-model:open="newExcelModalVisible" title="创建 Excel 文件" @ok="handleCreateExcel">
        <a-form :model="newExcelForm" layout="vertical">
          <a-form-item label="文件名" name="fileName">
            <a-input v-model:value="newExcelForm.fileName" placeholder="输入文件名" />
          </a-form-item>
          <a-form-item label="工作表" name="sheets">
            <a-select
              v-model:value="newExcelForm.sheets"
              mode="tags"
              style="width: 100%"
              placeholder="输入工作表名称并按回车"
            >
              <a-select-option v-for="sheet in defaultSheets" :key="sheet">{{ sheet }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </app-layout>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, reactive, h } from 'vue';
import { useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import { useAppStore } from '../stores/app';
import AppLayout from '../components/AppLayout.vue';
import logoUrl from '../assets/logo.png';
import { 
  FileOutlined, 
  CodeOutlined, 
  RightOutlined, 
  FileExcelOutlined,
  TableOutlined,
  BookOutlined,
  LaptopOutlined,
  InboxOutlined,
  EyeOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'HomeView',
  components: {
    AppLayout,
    FileOutlined,
    CodeOutlined,
    RightOutlined,
    FileExcelOutlined,
    TableOutlined,
    BookOutlined,
    LaptopOutlined,
    InboxOutlined,
    EyeOutlined,
    DeleteOutlined
  },
  setup() {
    const router = useRouter();
    const appStore = useAppStore();
    const recentFiles = ref([]);
    const loading = ref(false);
    const newExcelModalVisible = ref(false);
    const newExcelForm = reactive({
      fileName: 'new_data.xlsx',
      sheets: ['Sheet1', 'Sheet2']
    });
    const defaultSheets = ['Sheet1', 'Sheet2', 'Sheet3'];
    
    const appInfo = ref({
      version: '1.0.0',
      electronVersion: '-',
      nodeVersion: '-',
      platform: '-'
    });
    
    // 获取应用信息
    onMounted(() => {
      if (window.appInfo) {
        appInfo.value = window.appInfo;
      }
      
      loadRecentFiles();
    });
    
    // 加载最近文件
    const loadRecentFiles = async () => {
      loading.value = true;
      try {
        const files = await window.electronAPI.getRecentFiles();
        recentFiles.value = files || [];
      } catch (error) {
        console.error('加载最近文件失败:', error);
        message.error('加载最近文件失败');
      } finally {
        loading.value = false;
      }
    };
    
    // 打开Excel文件
    const openExcelFile = async () => {
      try {
        const filePath = await window.electronAPI.openFile();
        if (filePath) {
          router.push({ 
            path: '/workspace',
            query: { file: filePath }
          });
        }
      } catch (error) {
        message.error('打开文件失败: ' + error.message);
        console.error('打开文件错误:', error);
      }
    };
    
    // 打开已存在的文件
    const openExistingFile = (filePath) => {
      try {
        if (filePath) {
          router.push({ 
            path: '/workspace',
            query: { file: filePath }
          });
        }
      } catch (error) {
        message.error('打开文件失败: ' + error.message);
        console.error('打开文件错误:', error);
      }
    };
    
    // 创建新工作区
    const createNewWorkspace = () => {
      router.push('/workspace');
    };
    
    // 创建新Excel文件
    const createNewExcel = () => {
      newExcelModalVisible.value = true;
    };
    
    // 处理创建Excel文件
    const handleCreateExcel = async () => {
      if (!newExcelForm.fileName) {
        message.error('请输入文件名');
        return;
      }
      
      if (newExcelForm.sheets.length === 0) {
        message.error('请至少添加一个工作表');
        return;
      }
      
      message.loading('正在创建文件...');
      
      try {
        // 这里可以添加创建Excel文件的逻辑
        // 暂时模拟创建过程
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        newExcelModalVisible.value = false;
        message.success('文件创建成功');
        
        // 跳转到工作区
        router.push({
          path: '/workspace',
          query: { 
            newFile: true,
            fileName: newExcelForm.fileName,
            sheets: newExcelForm.sheets.join(',')
          }
        });
      } catch (error) {
        message.error('创建文件失败: ' + error.message);
        console.error('创建文件错误:', error);
      }
    };
    
    // 移除最近文件
    const removeRecentFile = async (file) => {
      Modal.confirm({
        title: '确认删除',
        content: `确定要从最近文件列表中移除 "${file.name}" 吗？`,
        okText: '确定',
        cancelText: '取消',
        onOk: async () => {
          try {
            const updatedFiles = recentFiles.value.filter(item => item.path !== file.path);
            await window.electronAPI.saveRecentFiles(updatedFiles);
            recentFiles.value = updatedFiles;
            message.success('已从最近文件列表中移除');
          } catch (error) {
            message.error('移除失败: ' + error.message);
            console.error('移除文件错误:', error);
          }
        }
      });
    };
    
    // 打开文档
    const openDocumentation = () => {
      message.info('文档功能即将推出');
    };
    
    // 显示SQL技巧
    const showSqlTips = () => {
      Modal.info({
        title: 'SQL 查询技巧',
        content: h('div', {
          innerHTML: `
            <p>1. 基本查询: <code>SELECT * FROM Sheet1</code></p>
            <p>2. 条件查询: <code>SELECT * FROM Sheet1 WHERE Column1 > 100</code></p>
            <p>3. 分组查询: <code>SELECT Column1, SUM(Column2) FROM Sheet1 GROUP BY Column1</code></p>
            <p>4. 排序: <code>SELECT * FROM Sheet1 ORDER BY Column1 DESC</code></p>
            <p>5. 连接查询: <code>SELECT a.*, b.* FROM Sheet1 a JOIN Sheet2 b ON a.ID = b.ID</code></p>
          `
        }),
        width: 600,
        class: 'sql-tips-modal'
      });
    };
    
    // 跳转到设置页面
    const goSettings = () => {
      router.push('/settings');
    };
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      
      try {
        const date = new Date(dateString);
        return date.toLocaleString();
      } catch (e) {
        return dateString;
      }
    };
    
    // 截断路径
    const truncatePath = (path) => {
      if (!path) return '';
      if (path.length <= 30) return path;
      
      const parts = path.split(/[\\\/]/);
      if (parts.length <= 2) return path;
      
      return '...' + path.substring(path.length - 30);
    };
    
    return {
      logoUrl,
      recentFiles,
      appInfo,
      loading,
      newExcelModalVisible,
      newExcelForm,
      defaultSheets,
      openExcelFile,
      openExistingFile,
      createNewWorkspace,
      createNewExcel,
      handleCreateExcel,
      removeRecentFile,
      openDocumentation,
      showSqlTips,
      goSettings,
      formatDate,
      truncatePath
    };
  }
});
</script>

<style scoped>
.home-container {
  height: 100%;
  width: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  background-color: var(--background-color);
}

.hero-section {
  background: linear-gradient(135deg, var(--primary-color), #722ed1);
  color: white;
  padding: 60px 20px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxkZWZzPjxwYXR0ZXJuIGlkPSJwYXR0ZXJuIiB4PSIwIiB5PSIwIiB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHBhdHRlcm5Vbml0cz0idXNlclNwYWNlT25Vc2UiIHBhdHRlcm5UcmFuc2Zvcm09InJvdGF0ZSgzMCkiPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyMCIgaGVpZ2h0PSIyMCIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjA1KSI+PC9yZWN0PjwvcGF0dGVybj48L2RlZnM+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNwYXR0ZXJuKSI+PC9yZWN0Pjwvc3ZnPg==');
  opacity: 0.1;
  z-index: 0;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
}

.logo-image {
  width: 120px;
  height: 120px;
  margin-bottom: 20px;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.1));
}

.app-title {
  font-size: 42px;
  font-weight: bold;
  margin-bottom: 16px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.app-description {
  font-size: 18px;
  margin-bottom: 40px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}

.main-action-btn {
  min-width: 180px;
  height: 48px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.features-section {
  padding: 70px 0px;
  margin: 0 auto;
  margin-top: -40px;
  position: relative;
  z-index: 2;
}

.feature-card {
  height: 100%;
  transition: all 0.3s;
  border-radius: var(--border-radius-base);
  overflow: hidden;
  box-shadow: var(--box-shadow-base);
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--box-shadow-card);
}

.card-icon-container {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.blue-gradient {
  background: linear-gradient(135deg, #1890ff, #096dd9);
}

.green-gradient {
  background: linear-gradient(135deg, #52c41a, #389e0d);
}

.purple-gradient {
  background: linear-gradient(135deg, #722ed1, #531dab);
}

.card-icon {
  font-size: 64px;
  color: white;
}

.recent-section {
  padding: 20px 0;
  margin: 0 auto;
}

.section-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 24px;
  position: relative;
  padding-left: 16px;
  color: var(--text-color);
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: var(--primary-color);
  border-radius: 2px;
}

.recent-list {
  margin-bottom: 40px;
}

.recent-card {
  height: 100%;
  transition: all 0.3s;
}

.recent-card:hover {
  transform: translateY(-3px);
}

.file-icon-container {
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f7ff;
}

.file-icon {
  font-size: 48px;
  color: #52c41a;
}

.file-path {
  color: var(--text-color-secondary);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.file-date {
  color: var(--text-color-secondary);
  font-size: 12px;
}

.empty-recent {
  padding: 40px 0;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: var(--disabled-color);
  margin-bottom: 16px;
}

.tips-section {
  margin: 0 auto;
  margin-bottom: 40px;
}

.tip-card {
  height: 100%;
  transition: all 0.3s;
}

.tip-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--box-shadow-base);
}

.tip-list {
  padding-left: 20px;
  margin-bottom: 16px;
  color: var(--text-color);
}

.tip-list li {
  margin-bottom: 8px;
}

.system-card :deep(.ant-descriptions-item-label) {
  color: var(--text-color-secondary);
  width: 120px;
}

@media (max-width: 768px) {
  .app-title {
    font-size: 32px;
  }
  
  .app-description {
    font-size: 16px;
  }
  
  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .main-action-btn {
    width: 100%;
    max-width: 300px;
  }
}
</style>

<style>
/* Global styles for SQL tips modal */
.sql-tips-modal .ant-modal-body {
  padding: 24px;
}

.sql-tips-modal .ant-modal-body p {
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 1.6;
}

.sql-tips-modal .ant-modal-body code {
  background-color: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', monospace;
  color: #c41d7f;
  font-size: 13px;
}

.dark-theme .sql-tips-modal .ant-modal-body code {
  background-color: rgba(255, 255, 255, 0.1);
  color: #ff85c0;
}
</style> 