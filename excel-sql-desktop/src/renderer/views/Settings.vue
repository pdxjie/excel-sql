<template>
  <app-layout :show-sider="true" :show-footer="false" :sider-collapsed="siderCollapsed">
    <template #sider>
      <div class="sider-container">
        <div class="logo">
          <setting-outlined />
          <span v-if="!siderCollapsed">设置</span>
        </div>
        <a-menu
          mode="inline"
          theme="dark"
          v-model:selectedKeys="selectedMenuKeys"
          class="settings-menu"
        >
          <a-menu-item key="general">
            <template #icon><appstore-outlined /></template>
            <span>常规设置</span>
          </a-menu-item>
          <a-menu-item key="appearance">
            <template #icon><skin-outlined /></template>
            <span>外观</span>
          </a-menu-item>
          <a-menu-item key="editor">
            <template #icon><code-outlined /></template>
            <span>编辑器</span>
          </a-menu-item>
          <a-menu-item key="data">
            <template #icon><database-outlined /></template>
            <span>数据</span>
          </a-menu-item>
          <a-menu-item key="api">
            <template #icon><api-outlined /></template>
            <span>API 设置</span>
          </a-menu-item>
          <a-menu-item key="shortcuts">
            <template #icon><thunderbolt-outlined /></template>
            <span>快捷键</span>
          </a-menu-item>
          <a-menu-item key="about">
            <template #icon><info-circle-outlined /></template>
            <span>关于</span>
          </a-menu-item>
        </a-menu>
        <div class="sidebar-footer">
          <a-button type="text" @click="goHome" class="sidebar-btn">
            <template #icon><home-outlined /></template>
            <span v-if="!siderCollapsed">返回首页</span>
          </a-button>
        </div>
      </div>
    </template>
    
    <template #header-left>
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
      <span class="header-title">{{ currentSettingTitle }}</span>
    </template>
    
    <div class="settings-content">
      <!-- General Settings -->
      <div v-show="selectedMenuKeys.includes('general')" class="setting-section">
        <h2 class="section-title">常规设置</h2>
        
        <a-card class="setting-card">
          <a-form layout="vertical">
            <a-form-item label="语言">
              <a-select v-model:value="settings.language" style="width: 200px">
                <a-select-option value="zh-CN">中文(简体)</a-select-option>
                <a-select-option value="en-US">English</a-select-option>
              </a-select>
            </a-form-item>
            
            <a-form-item label="启动选项">
              <a-radio-group v-model:value="settings.startupOption">
                <a-radio value="home">显示首页</a-radio>
                <a-radio value="last">打开上次的文件</a-radio>
                <a-radio value="new">创建新工作区</a-radio>
              </a-radio-group>
            </a-form-item>
            
            <a-form-item label="自动保存">
              <a-switch v-model:checked="settings.autoSave" />
              <span class="setting-description">每 {{ settings.autoSaveInterval }} 分钟自动保存</span>
            </a-form-item>
            
            <a-form-item v-if="settings.autoSave" label="自动保存间隔 (分钟)">
              <a-slider v-model:value="settings.autoSaveInterval" :min="1" :max="30" :step="1" style="width: 300px" />
            </a-form-item>
          </a-form>
        </a-card>
      </div>
      
      <!-- Appearance Settings -->
      <div v-show="selectedMenuKeys.includes('appearance')" class="setting-section">
        <h2 class="section-title">外观设置</h2>
        
        <a-card class="setting-card">
          <a-form layout="vertical">
            <a-form-item label="主题">
              <a-radio-group v-model:value="theme" button-style="solid" @change="handleThemeChange">
                <a-radio-button value="light">浅色</a-radio-button>
                <a-radio-button value="dark">深色</a-radio-button>
                <a-radio-button value="system">跟随系统</a-radio-button>
              </a-radio-group>
            </a-form-item>
            
            <a-form-item label="主色调">
              <div class="color-palette">
                <div 
                  v-for="color in colorOptions" 
                  :key="color.value"
                  :class="['color-option', primaryColor === color.value ? 'selected' : '']"
                  :style="{ backgroundColor: color.color }"
                  @click="handleColorChange(color.value)"
                ></div>
              </div>
            </a-form-item>
            
            <a-form-item label="字体大小">
              <a-slider v-model:value="settings.fontSize" :min="12" :max="20" :step="1" style="width: 300px" />
              <span class="setting-description">{{ settings.fontSize }}px</span>
            </a-form-item>
            
            <a-form-item label="紧凑模式">
              <a-switch v-model:checked="settings.compactMode" />
              <span class="setting-description">减小界面元素间距</span>
            </a-form-item>
          </a-form>
        </a-card>
      </div>
      
      <!-- Editor Settings -->
      <div v-show="selectedMenuKeys.includes('editor')" class="setting-section">
        <h2 class="section-title">编辑器设置</h2>
        
        <a-card class="setting-card">
          <a-form layout="vertical">
            <a-form-item label="代码编辑器主题">
              <a-select v-model:value="settings.editorTheme" style="width: 200px">
                <a-select-option value="vs">浅色</a-select-option>
                <a-select-option value="vs-dark">深色</a-select-option>
                <a-select-option value="hc-black">高对比度</a-select-option>
              </a-select>
            </a-form-item>
            
            <a-form-item label="字体大小">
              <a-slider v-model:value="settings.editorFontSize" :min="12" :max="24" :step="1" style="width: 300px" />
              <span class="setting-description">{{ settings.editorFontSize }}px</span>
            </a-form-item>
            
            <a-form-item label="Tab 大小">
              <a-slider v-model:value="settings.tabSize" :min="2" :max="8" :step="1" style="width: 300px" />
              <span class="setting-description">{{ settings.tabSize }} 个空格</span>
            </a-form-item>
            
            <a-form-item label="显示行号">
              <a-switch v-model:checked="settings.showLineNumbers" />
            </a-form-item>
            
            <a-form-item label="启用代码折叠">
              <a-switch v-model:checked="settings.enableFolding" />
            </a-form-item>
            
            <a-form-item label="启用小地图">
              <a-switch v-model:checked="settings.minimap" />
            </a-form-item>
          </a-form>
        </a-card>
      </div>
      
      <!-- Data Settings -->
      <div v-show="selectedMenuKeys.includes('data')" class="setting-section">
        <h2 class="section-title">数据设置</h2>
        
        <a-card class="setting-card">
          <a-form layout="vertical">
            <a-form-item label="文件存储路径">
              <div class="storage-path-input">
                <a-input v-model:value="storagePath" readonly style="width: calc(100% - 110px)" />
                <a-button type="primary" @click="selectStoragePath">
                  <template #icon><folder-outlined /></template>
                  选择目录
                </a-button>
              </div>
              <span class="setting-description">Excel 文件和查询结果的默认存储位置</span>
            </a-form-item>
            
            <a-form-item label="默认导出格式">
              <a-select v-model:value="settings.defaultExportFormat" style="width: 200px">
                <a-select-option value="xlsx">Excel (.xlsx)</a-select-option>
                <a-select-option value="csv">CSV (.csv)</a-select-option>
                <a-select-option value="json">JSON (.json)</a-select-option>
              </a-select>
            </a-form-item>
            
            <a-form-item label="最大查询结果行数">
              <a-input-number v-model:value="settings.maxResultRows" :min="100" :max="10000" :step="100" style="width: 200px" />
            </a-form-item>
            
            <a-form-item label="查询超时时间 (秒)">
              <a-slider v-model:value="settings.queryTimeout" :min="5" :max="120" :step="5" style="width: 300px" />
              <span class="setting-description">{{ settings.queryTimeout }}秒</span>
            </a-form-item>
            
            <a-divider>数据分析设置</a-divider>
            
            <a-form-item label="自动分析数据">
              <a-switch v-model:checked="settings.autoAnalyzeData" />
              <span class="setting-description">查询结果加载后自动进行数据分析</span>
            </a-form-item>
            
            <a-form-item label="异常值检测阈值">
              <a-slider v-model:value="settings.outlierThreshold" :min="1" :max="5" :step="0.5" style="width: 300px" />
              <span class="setting-description">标准差的倍数 ({{ settings.outlierThreshold }})</span>
            </a-form-item>
            
            <a-form-item label="数据采样大小">
              <a-input-number v-model:value="settings.sampleSize" :min="100" :max="10000" :step="100" style="width: 200px" />
              <span class="setting-description">大数据集分析时的采样大小</span>
            </a-form-item>
            
            <a-divider>图表设置</a-divider>
            
            <a-form-item label="默认图表类型">
              <a-select v-model:value="settings.defaultChartType" style="width: 200px">
                <a-select-option value="bar">柱状图</a-select-option>
                <a-select-option value="line">折线图</a-select-option>
                <a-select-option value="pie">饼图</a-select-option>
                <a-select-option value="scatter">散点图</a-select-option>
              </a-select>
            </a-form-item>
            
            <a-form-item label="图表主题">
              <a-radio-group v-model:value="settings.chartTheme">
                <a-radio value="default">默认</a-radio>
                <a-radio value="light">浅色</a-radio>
                <a-radio value="dark">深色</a-radio>
              </a-radio-group>
            </a-form-item>
            
            <a-form-item label="图表导出分辨率">
              <a-select v-model:value="settings.chartExportResolution" style="width: 200px">
                <a-select-option value="1">标准 (1x)</a-select-option>
                <a-select-option value="2">高清 (2x)</a-select-option>
                <a-select-option value="3">超高清 (3x)</a-select-option>
              </a-select>
            </a-form-item>
            
            <a-divider></a-divider>
            
            <a-form-item>
              <a-button type="primary" danger @click="clearRecentFiles">
                <template #icon><delete-outlined /></template>
                清除最近文件列表
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </div>
      
      <!-- API Settings -->
      <div v-show="selectedMenuKeys.includes('api')" class="setting-section">
        <h2 class="section-title">API 设置</h2>
        
        <a-card class="setting-card">
          <a-form layout="vertical">
            <a-form-item label="API 服务地址">
              <a-input v-model:value="apiBaseUrl" placeholder="http://localhost:8080/api" style="width: 100%" />
              <span class="setting-description">后端 API 服务的基础 URL</span>
            </a-form-item>
            
            <a-form-item>
              <a-button type="primary" @click="saveApiSettings">
                保存 API 设置
              </a-button>
              <a-button style="margin-left: 8px" @click="testApiConnection">
                测试连接
              </a-button>
            </a-form-item>
            
            <a-divider></a-divider>
            
            <a-form-item label="认证设置">
              <a-switch v-model:checked="settings.useAuth" />
              <span class="setting-description">启用 API 认证</span>
            </a-form-item>
            
            <template v-if="settings.useAuth">
              <a-form-item label="认证类型">
                <a-select v-model:value="settings.authType" style="width: 200px">
                  <a-select-option value="bearer">Bearer Token</a-select-option>
                  <a-select-option value="basic">Basic Auth</a-select-option>
                  <a-select-option value="apikey">API Key</a-select-option>
                </a-select>
              </a-form-item>
              
              <a-form-item v-if="settings.authType === 'apikey'" label="API Key 名称">
                <a-input v-model:value="settings.apiKeyName" placeholder="X-API-Key" style="width: 200px" />
              </a-form-item>
              
              <a-form-item v-if="settings.authType === 'apikey'" label="API Key 位置">
                <a-radio-group v-model:value="settings.apiKeyLocation">
                  <a-radio value="header">Header</a-radio>
                  <a-radio value="query">Query Parameter</a-radio>
                </a-radio-group>
              </a-form-item>
            </template>
          </a-form>
        </a-card>
      </div>
      
      <!-- Shortcuts Settings -->
      <div v-show="selectedMenuKeys.includes('shortcuts')" class="setting-section">
        <h2 class="section-title">快捷键设置</h2>
        
        <a-card class="setting-card">
          <a-table :columns="shortcutColumns" :data-source="shortcutData" :pagination="false" size="middle">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'shortcut'">
                <a-input v-model:value="record.shortcut" @focus="record.editing = true" @blur="record.editing = false" />
              </template>
              <template v-if="column.dataIndex === 'action'">
                <a-button size="small" @click="resetShortcut(record)">重置</a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </div>
      
      <!-- About -->
      <div v-show="selectedMenuKeys.includes('about')" class="setting-section">
        <h2 class="section-title">关于</h2>
        
        <a-card class="setting-card about-card">
          <div class="about-content">
            <img :src="logoUrl" alt="Excel SQL Logo" class="about-logo" />
            <h2>Excel SQL Desktop</h2>
            <p class="version">版本 {{ appInfo.version }}</p>
            
            <a-descriptions :column="1" size="small" class="about-info">
              <a-descriptions-item label="Electron 版本">{{ appInfo.electronVersion }}</a-descriptions-item>
              <a-descriptions-item label="Node.js 版本">{{ appInfo.nodeVersion }}</a-descriptions-item>
              <a-descriptions-item label="操作系统">{{ appInfo.platform }}</a-descriptions-item>
            </a-descriptions>
            
            <div class="about-actions">
              <a-button>检查更新</a-button>
              <a-button>许可协议</a-button>
              <a-button>隐私政策</a-button>
            </div>
            
            <p class="copyright">© 2023-2024 Excel SQL Desktop. 保留所有权利。</p>
          </div>
        </a-card>
      </div>
      
      <!-- Save Button -->
      <div class="settings-actions">
        <a-button type="primary" size="large" @click="saveSettings">
          保存设置
        </a-button>
        <a-button size="large" @click="resetSettings">
          重置为默认
        </a-button>
      </div>
    </div>
  </app-layout>
</template>

<script lang="ts">
import { defineComponent, ref, reactive, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import axios from 'axios';
import { useAppStore } from '../stores';
import AppLayout from '../components/AppLayout.vue';
import logoUrl from '../assets/logo.png';
import { 
  HomeOutlined,
  CodeOutlined,
  SettingOutlined,
  AppstoreOutlined,
  SkinOutlined,
  DatabaseOutlined,
  ThunderboltOutlined,
  InfoCircleOutlined,
  DeleteOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  FolderOutlined,
  ApiOutlined
} from '@ant-design/icons-vue';
import { api } from '../services';

interface Settings {
  theme: 'light' | 'dark' | 'system';
  fontSize: number;
  tabSize: 2 | 4;
  autoFormatSQL: boolean;
  wordWrap: boolean;
  language: 'en' | 'zh';
  defaultExportFormat: 'xlsx' | 'csv' | 'json';
  startupOption: 'home' | 'last' | 'new';
  autoSave: boolean;
  autoSaveInterval: number;
  primaryColor: string;
  compactMode: boolean;
  editorTheme: 'vs' | 'vs-dark' | 'hc-black';
  editorFontSize: number;
  showLineNumbers: boolean;
  enableFolding: boolean;
  minimap: boolean;
  maxResultRows: number;
  queryTimeout: number;
  autoAnalyzeData: boolean;
  outlierThreshold: number;
  sampleSize: number;
  defaultChartType: string;
  chartTheme: string;
  chartExportResolution: string;
  useAuth: boolean;
  authType: string;
  apiKeyName: string;
  apiKeyLocation: string;
}

const defaultSettings: Settings = {
  theme: 'light',
  fontSize: 14,
  tabSize: 2,
  autoFormatSQL: false,
  wordWrap: true,
  language: 'zh',
  defaultExportFormat: 'xlsx',
  startupOption: 'home',
  autoSave: true,
  autoSaveInterval: 5,
  primaryColor: 'blue',
  compactMode: false,
  editorTheme: 'vs-dark',
  editorFontSize: 14,
  showLineNumbers: true,
  enableFolding: true,
  minimap: true,
  maxResultRows: 1000,
  queryTimeout: 30,
  autoAnalyzeData: true,
  outlierThreshold: 2,
  sampleSize: 1000,
  defaultChartType: 'bar',
  chartTheme: 'default',
  chartExportResolution: '2',
  useAuth: false,
  authType: 'bearer',
  apiKeyName: 'X-API-Key',
  apiKeyLocation: 'header'
};

export default defineComponent({
  name: 'SettingsView',
  components: {
    AppLayout,
    HomeOutlined,
    CodeOutlined,
    SettingOutlined,
    AppstoreOutlined,
    SkinOutlined,
    DatabaseOutlined,
    ThunderboltOutlined,
    InfoCircleOutlined,
    DeleteOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    FolderOutlined,
    ApiOutlined
  },
  setup() {
    const router = useRouter();
    const appStore = useAppStore();
    const siderCollapsed = ref(false);
    const selectedMenuKeys = ref(['general']);
    
    // 应用信息
    const appInfo = ref({
      version: '1.0.0',
      electronVersion: '-',
      nodeVersion: '-',
      platform: '-'
    });
    
    // 主题和颜色
    const theme = ref(appStore.theme);
    const primaryColor = ref(appStore.primaryColor);
    
    // 设置
    const settings = reactive<Settings>({ ...defaultSettings });
    
    // 颜色选项
    const colorOptions = [
      { value: 'blue', color: '#1890ff' },
      { value: 'green', color: '#52c41a' },
      { value: 'purple', color: '#722ed1' },
      { value: 'red', color: '#f5222d' },
      { value: 'orange', color: '#fa8c16' },
      { value: 'cyan', color: '#13c2c2' }
    ];
    
    // 快捷键设置
    const shortcutColumns = [
      { title: '功能', dataIndex: 'feature', key: 'feature' },
      { title: '快捷键', dataIndex: 'shortcut', key: 'shortcut' },
      { title: '操作', dataIndex: 'action', key: 'action' }
    ];
    
    const shortcutData = ref([
      { key: '1', feature: '执行查询', shortcut: 'Ctrl+Enter', defaultShortcut: 'Ctrl+Enter', editing: false },
      { key: '2', feature: '格式化 SQL', shortcut: 'Ctrl+Shift+F', defaultShortcut: 'Ctrl+Shift+F', editing: false },
      { key: '3', feature: '保存查询', shortcut: 'Ctrl+S', defaultShortcut: 'Ctrl+S', editing: false },
      { key: '4', feature: '打开文件', shortcut: 'Ctrl+O', defaultShortcut: 'Ctrl+O', editing: false },
      { key: '5', feature: '新建工作区', shortcut: 'Ctrl+N', defaultShortcut: 'Ctrl+N', editing: false }
    ]);
    
    // 计算当前设置标题
    const currentSettingTitle = computed(() => {
      const titles = {
        general: '常规设置',
        appearance: '外观设置',
        editor: '编辑器设置',
        data: '数据设置',
        api: 'API 设置',
        shortcuts: '快捷键设置',
        about: '关于'
      };
      return titles[selectedMenuKeys.value[0]] || '设置';
    });
    
    // 初始化
    onMounted(() => {
      // 获取应用信息
      if (window.appInfo) {
        appInfo.value = window.appInfo;
      }
      loadStoragePath();
    });
    
    // 加载主题
    const loadTheme = () => {
      // 从 appStore 获取主题设置
      theme.value = appStore.theme;
      primaryColor.value = appStore.primaryColor;
    };
    
    // 主题变更
    const handleThemeChange = (e) => {
      theme.value = e.target.value;
      appStore.setTheme(theme.value);
    };
    
    // 颜色变更
    const handleColorChange = (color) => {
      primaryColor.value = color;
      appStore.setPrimaryColor(color);
    };
    
    // 保存设置
    const saveSettings = async () => {
      try {
        // 更新 Pinia store 中的设置
        appStore.settings = { ...settings };
        appStore.saveSettings();
        message.success('设置已保存');
      } catch (error) {
        console.error('保存设置失败:', error);
        message.error('保存设置失败');
      }
    };
    
    // 重置设置
    const resetSettings = () => {
      Modal.confirm({
        title: '重置设置',
        content: '确定要将所有设置重置为默认值吗？',
        okText: '确定',
        cancelText: '取消',
        onOk: () => {
          // 重置设置
          appStore.resetSettings();
          Object.assign(settings, appStore.settings);
          theme.value = appStore.theme;
          primaryColor.value = appStore.primaryColor;
          message.success('设置已重置为默认值');
        }
      });
    };
    
    // 重置快捷键
    const resetShortcut = (record) => {
      record.shortcut = record.defaultShortcut;
    };
    
    // 清除最近文件列表
    const clearRecentFiles = () => {
      Modal.confirm({
        title: '清除最近文件',
        content: '确定要清除最近文件列表吗？',
        okText: '确定',
        cancelText: '取消',
        onOk: async () => {
          try {
            await window.electronAPI.saveRecentFiles([]);
            message.success('最近文件列表已清除');
          } catch (error) {
            message.error('清除失败: ' + error.message);
            console.error('清除最近文件错误:', error);
          }
        }
      });
    };
    
    // 导航
    const goHome = () => {
      router.push('/');
    };
    
    // 选择存储路径
    const storagePath = ref('');
    
    // 加载存储路径
    const loadStoragePath = async () => {
      try {
        const path = await window.electronAPI.getStoragePath();
        storagePath.value = path;
      } catch (error) {
        console.error('Failed to load storage path:', error);
        message.error('加载存储路径失败');
      }
    };
    
    // 选择存储路径
    const selectStoragePath = async () => {
      try {
        const path = await window.electronAPI.selectDirectory();
        if (path) {
          storagePath.value = path;
          const result = await window.electronAPI.setStoragePath(path);
          if (result.success) {
            message.success('存储路径已更新');
          } else {
            message.error('更新存储路径失败: ' + result.error);
          }
        }
      } catch (error) {
        console.error('Failed to select storage path:', error);
        message.error('选择存储路径失败');
      }
    };
    
    // API 设置
    const apiBaseUrl = ref(appStore.apiBaseUrl);
    
    // 保存 API 设置
    const saveApiSettings = () => {
      appStore.setApiBaseUrl(apiBaseUrl.value);
      message.success('API 设置已保存');
    };
    
    // 测试 API 连接
    const testApiConnection = async () => {
      try {
        // 临时设置 baseURL 进行测试
        const testApi = axios.create({
          baseURL: apiBaseUrl.value,
          timeout: 5000
        });
        
        // 尝试请求 API 健康检查端点
        await testApi.get('/health');
        message.success('API 连接成功');
      } catch (error) {
        console.error('API 连接失败:', error);
        message.error('API 连接失败: ' + (error.message || '无法连接到服务器'));
      }
    };
    
    return {
      logoUrl,
      siderCollapsed,
      selectedMenuKeys,
      settings,
      appInfo,
      theme,
      primaryColor,
      colorOptions,
      shortcutColumns,
      shortcutData,
      currentSettingTitle,
      handleThemeChange,
      handleColorChange,
      saveSettings,
      resetSettings,
      resetShortcut,
      clearRecentFiles,
      goHome,
      storagePath,
      selectStoragePath,
      apiBaseUrl,
      saveApiSettings,
      testApiConnection
    };
  }
});
</script>

<style scoped>
.settings-content {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  overflow-y: auto;
  height: calc(100vh - 64px); /* 减去header高度 */
}

.setting-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 20px;
  font-weight: 500;
  margin-bottom: 16px;
  position: relative;
  padding-left: 12px;
  color: var(--text-color);
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: var(--primary-color);
  border-radius: 2px;
}

.setting-card {
  margin-bottom: 16px;
  border-radius: var(--border-radius-base);
}

/* 确保设置页面中的表格可以滚动 */
.setting-card :deep(.ant-table-body) {
  overflow-y: auto !important;
  max-height: 400px !important;
}

.setting-description {
  margin-left: 8px;
  color: var(--text-color-secondary);
}

.color-palette {
  display: flex;
  gap: 12px;
}

.color-option {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.selected::after {
  content: '';
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border: 2px solid var(--primary-color);
  border-radius: 6px;
}

.settings-actions {
  display: flex;
  justify-content: flex-start;
  gap: 16px;
  margin-top: 32px;
  margin-bottom: 48px;
}

.about-card {
  text-align: center;
}

.about-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.about-logo {
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.version {
  color: var(--text-color-secondary);
  margin-bottom: 24px;
}

.about-info {
  margin-bottom: 24px;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}

.about-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
  justify-content: center;
}

.copyright {
  color: var(--text-color-secondary);
  font-size: 12px;
}

.sider-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.logo {
  height: 64px;
  padding: 0 16px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  background-color: #002140;
  overflow: hidden;
  flex-shrink: 0;
}

.settings-menu {
  flex: 1;
  background-color: transparent;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.settings-menu :deep(.ant-menu-item) {
  margin: 0;
  height: 50px;
  line-height: 50px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.sidebar-btn {
  width: 100%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.storage-path-input {
  display: flex;
  gap: 8px;
}

.header-title {
  color: #fff;
  font-size: 16px;
  margin-left: 16px;
}

.trigger {
  font-size: 18px;
  line-height: 64px;
  padding: 0 24px;
  cursor: pointer;
  transition: color 0.3s;
  color: #fff;
}

.trigger:hover {
  color: var(--primary-color);
}
</style> 