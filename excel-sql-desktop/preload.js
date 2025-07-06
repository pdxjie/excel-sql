// preload.js
const { contextBridge, ipcRenderer } = require('electron');

// 暴露安全的 Electron API 给渲染进程
contextBridge.exposeInMainWorld('electronAPI', {
  // 文件对话框操作
  openFile: () => ipcRenderer.invoke('open-file-dialog'),
  saveFile: (options) => ipcRenderer.invoke('save-file-dialog', options),
  selectDirectory: () => ipcRenderer.invoke('select-directory-dialog'),
  
  // 文件读写操作
  readFile: (filePath) => ipcRenderer.invoke('read-file', filePath),
  writeFile: (filePath, data) => ipcRenderer.invoke('write-file', filePath, data),
  
  // 最近文件操作
  getRecentFiles: () => ipcRenderer.invoke('get-recent-files'),
  saveRecentFiles: (files) => ipcRenderer.invoke('save-recent-files', files),
  
  // 存储路径操作
  getStoragePath: () => ipcRenderer.invoke('get-storage-path'),
  setStoragePath: (path) => ipcRenderer.invoke('set-storage-path', path),
  
  // 主题相关
  onThemeChange: (callback) => {
    ipcRenderer.on('theme-change', callback);
    return () => ipcRenderer.removeListener('theme-change', callback);
  },
  setTheme: (theme) => ipcRenderer.invoke('set-theme', theme),
  
  // API 请求 - 使用主进程处理
  apiRequest: (config) => ipcRenderer.invoke('api-request', config)
});

// 添加版本信息
contextBridge.exposeInMainWorld('appInfo', {
  version: process.env.npm_package_version || '1.0.0',
  electronVersion: process.versions.electron,
  nodeVersion: process.versions.node,
  platform: process.platform
});

// 添加开发工具
if (process.env.NODE_ENV === 'development') {
  console.log('Running in development mode');
  console.log('Electron API exposed:', Object.keys(contextBridge));
} 