const { app, BrowserWindow, ipcMain, dialog, session } = require('electron');
const path = require('path');
const fs = require('fs');
const axios = require('axios');

// 移除对 electron-squirrel-startup 的引用
// 如果需要处理 Windows 安装/卸载快捷方式，可以在安装该模块后取消注释
// if (require('electron-squirrel-startup')) {
//   app.quit();
// }

let mainWindow = null;

const createWindow = () => {
  // 设置 CSP 以允许 API 请求
  session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
    callback({
      responseHeaders: {
        ...details.responseHeaders,
        'Content-Security-Policy': [
          "default-src 'self'; connect-src 'self' http://localhost:* http://127.0.0.1:* ws://localhost:*; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';"
        ]
      }
    });
  });

  // 创建浏览器窗口
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      enableRemoteModule: false,
      preload: path.join(__dirname, 'preload.js'),
      webSecurity: false, // 允许加载本地资源
    },
    icon: path.join(__dirname, 'src/renderer/assets/logo.png')
  });

  // 加载应用的 index.html
  const isDev = process.env.NODE_ENV === 'development' || !app.isPackaged;
  if (isDev) {
    // 开发模式下，尝试多个端口
    const tryLoadURL = (port) => {
      mainWindow.loadURL(`http://localhost:${port}`)
        .catch(() => {
          if (port < 3010) {
            tryLoadURL(port + 1);
          } else {
            console.error('Failed to connect to dev server');
            mainWindow.loadFile(path.join(__dirname, './dist/index.html'));
          }
        });
    };
    
    tryLoadURL(3000);
    // 打开开发者工具
    mainWindow.webContents.openDevTools();
    console.log('Running in development mode');
  } else {
    mainWindow.loadFile(path.join(__dirname, './dist/index.html'));
    console.log('Running in production mode');
  }

  // 设置事件处理程序
  setupIpcHandlers();
};

// 当 Electron 完成初始化时调用此方法
app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    // 在 macOS 上，当点击 dock 图标并且没有其他窗口打开时，
    // 通常在应用程序中重新创建一个窗口。
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

// 当所有窗口关闭时退出应用，除了在 macOS 上
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// 设置 IPC 处理程序用于文件操作
function setupIpcHandlers() {
  // API 请求处理
  ipcMain.handle('api-request', async (event, config) => {
    console.log('API Request:', config.method, config.url || config.baseURL + config.url);
    try {
      const response = await axios(config);
      console.log('API Response status:', response.status);
      
      // 对于大型响应，不要在控制台打印完整内容
      if (response.data) {
        if (Array.isArray(response.data)) {
          console.log(`API Response data: Array with ${response.data.length} items`);
        } else if (typeof response.data === 'object') {
          console.log('API Response data keys:', Object.keys(response.data));
        } else {
          console.log('API Response data type:', typeof response.data);
        }
      }
      
      return {
        data: response.data,
        status: response.status,
        headers: response.headers,
        success: true
      };
    } catch (error) {
      console.error('API request error:', error.message);
      return {
        error: error.message,
        status: error.response?.status,
        data: error.response?.data,
        success: false
      };
    }
  });

  // 打开文件对话框
  ipcMain.handle('open-file-dialog', async (event, fileTypes) => {
    console.log('Opening file dialog');
    try {
      const result = await dialog.showOpenDialog(mainWindow, {
        properties: ['openFile'],
        filters: [
          { name: 'Excel Files', extensions: ['xlsx', 'xls', 'csv'] },
          { name: 'All Files', extensions: ['*'] }
        ]
      });
      
      console.log('Dialog result:', result);
      if (!result.canceled && result.filePaths.length > 0) {
        // 添加到最近文件列表
        addToRecentFiles(result.filePaths[0]);
        return result.filePaths[0];
      }
      return null;
    } catch (error) {
      console.error('Error opening file dialog:', error);
      return null;
    }
  });

  // 选择目录对话框
  ipcMain.handle('select-directory-dialog', async () => {
    console.log('Opening directory dialog');
    try {
      const result = await dialog.showOpenDialog(mainWindow, {
        properties: ['openDirectory', 'createDirectory']
      });
      
      console.log('Directory dialog result:', result);
      if (!result.canceled && result.filePaths.length > 0) {
        return result.filePaths[0];
      }
      return null;
    } catch (error) {
      console.error('Error opening directory dialog:', error);
      return null;
    }
  });

  // 保存文件对话框
  ipcMain.handle('save-file-dialog', async (event, options) => {
    console.log('Opening save dialog', options);
    try {
      const { defaultPath, filters } = options || {};
      const result = await dialog.showSaveDialog(mainWindow, {
        defaultPath,
        filters: filters || [
          { name: 'Excel Files', extensions: ['xlsx'] },
          { name: 'CSV Files', extensions: ['csv'] },
          { name: 'JSON Files', extensions: ['json'] }
        ]
      });
      
      console.log('Save dialog result:', result);
      if (!result.canceled && result.filePath) {
        return result.filePath;
      }
      return null;
    } catch (error) {
      console.error('Error opening save dialog:', error);
      return null;
    }
  });

  // 读取文件
  ipcMain.handle('read-file', async (event, filePath) => {
    console.log('Reading file:', filePath);
    try {
      const data = fs.readFileSync(filePath);
      return { success: true, data: data.toString('base64') };
    } catch (error) {
      console.error('Error reading file:', error);
      return { success: false, error: error.message };
    }
  });

  // 写入文件
  ipcMain.handle('write-file', async (event, filePath, data) => {
    console.log('Writing file:', filePath);
    try {
      fs.writeFileSync(filePath, Buffer.from(data, 'base64'));
      return { success: true };
    } catch (error) {
      console.error('Error writing file:', error);
      return { success: false, error: error.message };
    }
  });

  // 获取最近使用的文件
  ipcMain.handle('get-recent-files', async () => {
    console.log('Getting recent files');
    try {
      const userDataPath = app.getPath('userData');
      const recentFilesPath = path.join(userDataPath, 'recent-files.json');
      
      if (fs.existsSync(recentFilesPath)) {
        const data = fs.readFileSync(recentFilesPath, 'utf8');
        return JSON.parse(data);
      }
      return [];
    } catch (error) {
      console.error('Failed to get recent files:', error);
      return [];
    }
  });

  // 保存最近使用的文件
  ipcMain.handle('save-recent-files', async (event, recentFiles) => {
    console.log('Saving recent files');
    try {
      // 确保 recentFiles 是一个简单的数组
      if (!Array.isArray(recentFiles)) {
        console.error('Invalid recent files format, expected array');
        return { success: false, error: 'Invalid format' };
      }
      
      // 只保留简单属性
      const simplifiedFiles = recentFiles.map(file => ({
        name: file.name || '',
        path: file.path || '',
        lastOpened: file.lastOpened || new Date().toISOString()
      }));
      
      const userDataPath = app.getPath('userData');
      const recentFilesPath = path.join(userDataPath, 'recent-files.json');
      
      fs.writeFileSync(recentFilesPath, JSON.stringify(simplifiedFiles, null, 2));
      return { success: true };
    } catch (error) {
      console.error('Failed to save recent files:', error);
      return { success: false, error: error.message };
    }
  });

  // 获取存储路径
  ipcMain.handle('get-storage-path', async () => {
    console.log('Getting storage path');
    try {
      const userDataPath = app.getPath('userData');
      const configPath = path.join(userDataPath, 'config.json');
      
      if (fs.existsSync(configPath)) {
        const data = fs.readFileSync(configPath, 'utf8');
        const config = JSON.parse(data);
        return config.storagePath || app.getPath('documents');
      }
      return app.getPath('documents'); // 默认使用文档目录
    } catch (error) {
      console.error('Failed to get storage path:', error);
      return app.getPath('documents');
    }
  });

  // 设置存储路径
  ipcMain.handle('set-storage-path', async (event, storagePath) => {
    console.log('Setting storage path:', storagePath);
    try {
      if (!storagePath || typeof storagePath !== 'string') {
        return { success: false, error: 'Invalid storage path' };
      }
      
      // 确保目录存在
      if (!fs.existsSync(storagePath)) {
        fs.mkdirSync(storagePath, { recursive: true });
      }
      
      const userDataPath = app.getPath('userData');
      const configPath = path.join(userDataPath, 'config.json');
      
      // 读取现有配置或创建新配置
      let config = {};
      if (fs.existsSync(configPath)) {
        const data = fs.readFileSync(configPath, 'utf8');
        config = JSON.parse(data);
      }
      
      // 更新存储路径
      config.storagePath = storagePath;
      
      // 保存配置
      fs.writeFileSync(configPath, JSON.stringify(config, null, 2));
      return { success: true };
    } catch (error) {
      console.error('Failed to set storage path:', error);
      return { success: false, error: error.message };
    }
  });

  // 主题相关
  ipcMain.handle('set-theme', async (event, theme) => {
    console.log('Setting theme:', theme);
    try {
      // 将主题变更通知发送给所有窗口
      BrowserWindow.getAllWindows().forEach(window => {
        window.webContents.send('theme-change', theme);
      });
      return { success: true };
    } catch (error) {
      console.error('Error setting theme:', error);
      return { success: false, error: error.message };
    }
  });
}

// 添加到最近文件列表
async function addToRecentFiles(filePath) {
  try {
    if (!filePath) return;
    
    const fileName = path.basename(filePath);
    const userDataPath = app.getPath('userData');
    const recentFilesPath = path.join(userDataPath, 'recent-files.json');
    
    let recentFiles = [];
    if (fs.existsSync(recentFilesPath)) {
      const data = fs.readFileSync(recentFilesPath, 'utf8');
      recentFiles = JSON.parse(data);
    }
    
    // 检查文件是否已存在于列表中
    const existingIndex = recentFiles.findIndex(file => file.path === filePath);
    if (existingIndex !== -1) {
      // 如果存在，更新最后打开时间并移到列表顶部
      recentFiles.splice(existingIndex, 1);
    }
    
    // 添加到列表顶部
    recentFiles.unshift({
      name: fileName,
      path: filePath,
      lastOpened: new Date().toISOString()
    });
    
    // 限制列表大小为10个
    if (recentFiles.length > 10) {
      recentFiles = recentFiles.slice(0, 10);
    }
    
    // 保存更新后的列表
    fs.writeFileSync(recentFilesPath, JSON.stringify(recentFiles, null, 2));
  } catch (error) {
    console.error('Failed to add to recent files:', error);
  }
} 