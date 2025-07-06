import { app, BrowserWindow, ipcMain, dialog, IpcMainInvokeEvent } from 'electron';
import { join } from 'path';
import * as fs from 'fs';

// Handle creating/removing shortcuts on Windows when installing/uninstalling
if (require('electron-squirrel-startup')) {
  app.quit();
}

let mainWindow: BrowserWindow | null = null;

const createWindow = () => {
  // Create the browser window
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      preload: join(__dirname, 'preload.js'),
    },
  });

  // Load the index.html of the app
  if (process.env.NODE_ENV === 'development') {
    mainWindow.loadURL('http://localhost:3000');
    // Open the DevTools
    mainWindow.webContents.openDevTools();
  } else {
    mainWindow.loadFile(join(__dirname, '../../dist/index.html'));
  }

  // Set up event handlers
  setupIpcHandlers();
};

// This method will be called when Electron has finished initialization
app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    // On macOS it's common to re-create a window when the dock icon is clicked
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

// Quit when all windows are closed, except on macOS
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

interface SaveDialogOptions {
  defaultPath?: string;
  filters?: { name: string; extensions: string[] }[];
}

// Set up IPC handlers for file operations
function setupIpcHandlers() {
  // Open file dialog
  ipcMain.handle('open-file-dialog', async (event: IpcMainInvokeEvent, fileTypes?: string[]) => {
    const result = await dialog.showOpenDialog(mainWindow!, {
      properties: ['openFile'],
      filters: [
        { name: 'Excel Files', extensions: ['xlsx', 'xls', 'csv'] },
        { name: 'All Files', extensions: ['*'] }
      ]
    });
    
    if (!result.canceled && result.filePaths.length > 0) {
      return result.filePaths[0];
    }
    return null;
  });

  // Save file dialog
  ipcMain.handle('save-file-dialog', async (event: IpcMainInvokeEvent, options: SaveDialogOptions) => {
    const { defaultPath, filters } = options;
    const result = await dialog.showSaveDialog(mainWindow!, {
      defaultPath,
      filters: filters || [
        { name: 'Excel Files', extensions: ['xlsx'] },
        { name: 'CSV Files', extensions: ['csv'] },
        { name: 'JSON Files', extensions: ['json'] }
      ]
    });
    
    if (!result.canceled && result.filePath) {
      return result.filePath;
    }
    return null;
  });

  // Read file
  ipcMain.handle('read-file', async (event: IpcMainInvokeEvent, filePath: string) => {
    try {
      const data = fs.readFileSync(filePath);
      return { success: true, data: data.toString('base64') };
    } catch (error) {
      return { success: false, error: (error as Error).message };
    }
  });

  // Write file
  ipcMain.handle('write-file', async (event: IpcMainInvokeEvent, filePath: string, data: string) => {
    try {
      fs.writeFileSync(filePath, Buffer.from(data, 'base64'));
      return { success: true };
    } catch (error) {
      return { success: false, error: (error as Error).message };
    }
  });

  // Get recent files
  ipcMain.handle('get-recent-files', async () => {
    try {
      const userDataPath = app.getPath('userData');
      const recentFilesPath = join(userDataPath, 'recent-files.json');
      
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

  // Save recent files
  ipcMain.handle('save-recent-files', async (event: IpcMainInvokeEvent, recentFiles: any[]) => {
    try {
      const userDataPath = app.getPath('userData');
      const recentFilesPath = join(userDataPath, 'recent-files.json');
      
      fs.writeFileSync(recentFilesPath, JSON.stringify(recentFiles, null, 2));
      return { success: true };
    } catch (error) {
      console.error('Failed to save recent files:', error);
      return { success: false, error: (error as Error).message };
    }
  });
} 