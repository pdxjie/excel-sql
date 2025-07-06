import { defineStore } from 'pinia';

interface Sheet {
  name: string;
  data: any[];
}

interface RecentFile {
  name: string;
  path: string;
  lastOpened: string;
}

interface Settings {
  theme: 'light' | 'dark' | 'system';
  fontSize: number;
  tabSize: 2 | 4;
  autoFormatSQL: boolean;
  wordWrap: boolean;
  language: 'en' | 'zh';
  defaultExportFormat: 'xlsx' | 'csv' | 'json';
}

export const useAppStore = defineStore('app', {
  state: () => ({
    currentFile: null as string | null,
    sheets: [] as Sheet[],
    recentFiles: [] as RecentFile[],
    settings: {
      theme: 'light',
      fontSize: 14,
      tabSize: 2,
      autoFormatSQL: false,
      wordWrap: true,
      language: 'en',
      defaultExportFormat: 'xlsx'
    } as Settings
  }),
  
  actions: {
    async loadRecentFiles() {
      try {
        // @ts-ignore - Electron API is injected via preload script
        const files = await window.electronAPI.getRecentFiles();
        this.recentFiles = files || [];
      } catch (error) {
        console.error('Failed to load recent files:', error);
      }
    },
    
    async addRecentFile(filePath: string) {
      const fileName = filePath.split(/[\\\/]/).pop() || '';
      const newRecentFile: RecentFile = {
        name: fileName,
        path: filePath,
        lastOpened: new Date().toISOString()
      };
      
      // Add to the beginning of the list, remove duplicates
      this.recentFiles = [
        newRecentFile,
        ...this.recentFiles.filter(file => file.path !== filePath)
      ].slice(0, 10); // Keep only 10 most recent
      
      // Save to storage
      try {
        // @ts-ignore - Electron API is injected via preload script
        await window.electronAPI.saveRecentFiles(this.recentFiles);
      } catch (error) {
        console.error('Failed to save recent files:', error);
      }
    },
    
    async removeRecentFile(filePath: string) {
      this.recentFiles = this.recentFiles.filter(file => file.path !== filePath);
      
      // Save to storage
      try {
        // @ts-ignore - Electron API is injected via preload script
        await window.electronAPI.saveRecentFiles(this.recentFiles);
      } catch (error) {
        console.error('Failed to save recent files:', error);
      }
    },
    
    async clearRecentFiles() {
      this.recentFiles = [];
      
      // Save to storage
      try {
        // @ts-ignore - Electron API is injected via preload script
        await window.electronAPI.saveRecentFiles(this.recentFiles);
      } catch (error) {
        console.error('Failed to save recent files:', error);
      }
    },
    
    setCurrentFile(filePath: string | null) {
      this.currentFile = filePath;
    },
    
    setSheets(sheets: Sheet[]) {
      this.sheets = sheets;
    },
    
    updateSettings(settings: Partial<Settings>) {
      this.settings = { ...this.settings, ...settings };
      
      // Save settings to localStorage
      try {
        localStorage.setItem('settings', JSON.stringify(this.settings));
      } catch (error) {
        console.error('Failed to save settings:', error);
      }
      
      // Apply theme
      this.applyTheme();
    },
    
    loadSettings() {
      try {
        const storedSettings = localStorage.getItem('settings');
        
        if (storedSettings) {
          const parsedSettings = JSON.parse(storedSettings);
          this.settings = { ...this.settings, ...parsedSettings };
        }
        
        // Apply theme
        this.applyTheme();
      } catch (error) {
        console.error('Failed to load settings:', error);
      }
    },
    
    applyTheme() {
      if (this.settings.theme === 'dark') {
        document.documentElement.classList.add('dark-theme');
      } else if (this.settings.theme === 'light') {
        document.documentElement.classList.remove('dark-theme');
      } else {
        // System theme
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (prefersDark) {
          document.documentElement.classList.add('dark-theme');
        } else {
          document.documentElement.classList.remove('dark-theme');
        }
      }
    }
  }
}); 