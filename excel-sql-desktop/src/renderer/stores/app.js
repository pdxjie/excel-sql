import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useAppStore = defineStore('app', () => {
  // 主题相关
  const theme = ref(localStorage.getItem('theme') || 'light');
  const primaryColor = ref(localStorage.getItem('primaryColor') || 'blue');
  
  // 设置主题
  const setTheme = (newTheme) => {
    theme.value = newTheme;
    localStorage.setItem('theme', newTheme);
    
    // 应用主题
    if (newTheme === 'dark') {
      document.documentElement.classList.add('dark-theme');
    } else if (newTheme === 'light') {
      document.documentElement.classList.remove('dark-theme');
    } else if (newTheme === 'system') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      if (prefersDark) {
        document.documentElement.classList.add('dark-theme');
      } else {
        document.documentElement.classList.remove('dark-theme');
      }
    }
  };
  
  // 设置主色调
  const setPrimaryColor = (newColor) => {
    primaryColor.value = newColor;
    localStorage.setItem('primaryColor', newColor);
    
    // 这里可以添加更改主色调的逻辑
  };
  
  // 编辑器相关
  const editorTheme = computed(() => {
    return theme.value === 'dark' ? 'vs-dark' : 'vs';
  });
  
  // 应用设置
  const settings = ref({
    language: 'zh-CN',
    fontSize: 14,
    editorFontSize: 14,
    tabSize: 2,
    autoSave: true,
    autoSaveInterval: 5,
    showLineNumbers: true,
    wordWrap: true,
    minimap: true,
    defaultExportFormat: 'xlsx',
    maxResultRows: 1000,
    queryTimeout: 30
  });
  
  // 加载设置
  const loadSettings = () => {
    const savedSettings = localStorage.getItem('settings');
    if (savedSettings) {
      try {
        settings.value = JSON.parse(savedSettings);
      } catch (e) {
        console.error('解析设置失败:', e);
      }
    }
  };
  
  // 保存设置
  const saveSettings = () => {
    localStorage.setItem('settings', JSON.stringify(settings.value));
  };
  
  // 重置设置
  const resetSettings = () => {
    settings.value = {
      language: 'zh-CN',
      fontSize: 14,
      editorFontSize: 14,
      tabSize: 2,
      autoSave: true,
      autoSaveInterval: 5,
      showLineNumbers: true,
      wordWrap: true,
      minimap: true,
      defaultExportFormat: 'xlsx',
      maxResultRows: 1000,
      queryTimeout: 30
    };
    saveSettings();
  };
  
  // 工作区相关
  const currentFile = ref(null);
  const currentWorkspace = ref({
    file: null,
    sheets: [],
    currentSheet: null,
    sql: '',
    results: null,
    error: null,
    loading: false
  });
  
  // 设置当前文件
  const setCurrentFile = (file) => {
    currentFile.value = file;
    currentWorkspace.value.file = file;
  };
  
  // 设置工作表
  const setSheets = (sheets) => {
    currentWorkspace.value.sheets = sheets;
    if (sheets.length > 0 && !currentWorkspace.value.currentSheet) {
      currentWorkspace.value.currentSheet = sheets[0];
    }
  };
  
  // 设置当前工作表
  const setCurrentSheet = (sheet) => {
    currentWorkspace.value.currentSheet = sheet;
  };
  
  // 设置 SQL 查询
  const setSql = (sql) => {
    currentWorkspace.value.sql = sql;
  };
  
  // 设置查询结果
  const setResults = (results) => {
    currentWorkspace.value.results = results;
  };
  
  // 设置错误信息
  const setError = (error) => {
    currentWorkspace.value.error = error;
  };
  
  // 设置加载状态
  const setLoading = (loading) => {
    currentWorkspace.value.loading = loading;
  };
  
  // 重置工作区
  const resetWorkspace = () => {
    currentWorkspace.value = {
      file: null,
      sheets: [],
      currentSheet: null,
      sql: '',
      results: null,
      error: null,
      loading: false
    };
  };
  
  // 初始化
  loadSettings();
  setTheme(theme.value);
  
  return {
    theme,
    primaryColor,
    setTheme,
    setPrimaryColor,
    editorTheme,
    settings,
    loadSettings,
    saveSettings,
    resetSettings,
    currentFile,
    currentWorkspace,
    setCurrentFile,
    setSheets,
    setCurrentSheet,
    setSql,
    setResults,
    setError,
    setLoading,
    resetWorkspace
  };
}); 