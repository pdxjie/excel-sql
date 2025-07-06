// Preload script
const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electronAPI', {
  // File operations
  openFile: () => ipcRenderer.invoke('open-file-dialog'),
  saveFile: (options) => ipcRenderer.invoke('save-file-dialog', options),
  readFile: (filePath) => ipcRenderer.invoke('read-file', filePath),
  writeFile: (filePath, data) => ipcRenderer.invoke('write-file', filePath, data),
  
  // Recent files
  getRecentFiles: () => ipcRenderer.invoke('get-recent-files'),
  saveRecentFiles: (recentFiles) => ipcRenderer.invoke('save-recent-files', recentFiles)
}); 