import { AxiosRequestConfig } from 'axios';

declare global {
  interface Window {
    electronAPI: {
      openFile: () => Promise<string | null>;
      saveFile: (options?: any) => Promise<string | null>;
      selectDirectory: () => Promise<string | null>;
      readFile: (filePath: string) => Promise<any>;
      writeFile: (filePath: string, data: any) => Promise<any>;
      getRecentFiles: () => Promise<any[]>;
      saveRecentFiles: (files: any[]) => Promise<any>;
      getStoragePath: () => Promise<string>;
      setStoragePath: (path: string) => Promise<any>;
      onThemeChange: (callback: (event: any, theme: string) => void) => () => void;
      setTheme: (theme: string) => Promise<any>;
      apiRequest: (config: AxiosRequestConfig) => Promise<{
        data: any;
        status: number;
        headers: any;
        success: boolean;
        error?: string;
      }>;
    };
    appInfo: {
      version: string;
      electronVersion: string;
      nodeVersion: string;
      platform: string;
    };
  }
}

export {};

declare module 'electron' {
  import { EventEmitter } from 'events';

  export interface App extends EventEmitter {
    quit: () => void;
    getPath: (name: string) => string;
    whenReady: () => Promise<void>;
    on: (event: string, listener: (...args: any[]) => void) => this;
  }

  export interface BrowserWindow {
    loadURL: (url: string) => Promise<void>;
    loadFile: (path: string) => Promise<void>;
    webContents: {
      openDevTools: () => void;
    };
  }

  export interface Dialog {
    showOpenDialog: (window: BrowserWindow, options: any) => Promise<{ canceled: boolean; filePaths: string[] }>;
    showSaveDialog: (window: BrowserWindow, options: any) => Promise<{ canceled: boolean; filePath?: string }>;
  }

  export interface IpcMain {
    handle: (channel: string, listener: (event: IpcMainInvokeEvent, ...args: any[]) => Promise<any>) => void;
  }

  export interface IpcMainInvokeEvent {
    sender: any;
  }

  export const app: App;
  export const BrowserWindow: { new(options: any): BrowserWindow; getAllWindows: () => BrowserWindow[] };
  export const dialog: Dialog;
  export const ipcMain: IpcMain;
} 