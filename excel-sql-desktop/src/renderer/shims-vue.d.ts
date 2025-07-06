declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface Window {
  electronAPI?: {
    openFile: () => Promise<string | null>;
    saveFile: (options?: any) => Promise<string | null>;
    readFile: (filePath: string) => Promise<any>;
    writeFile: (filePath: string, data: any) => Promise<any>;
    getRecentFiles: () => Promise<any[]>;
    saveRecentFiles: (files: any[]) => Promise<any>;
    onThemeChange: (callback: (event: any, theme: string) => void) => void;
    setTheme: (theme: string) => Promise<any>;
    apiRequest: (config: any) => Promise<{
      data: any;
      status: number;
      headers: any;
      success: boolean;
      error?: string;
    }>;
  };
  appInfo?: {
    version: string;
    electronVersion: string;
    nodeVersion: string;
    platform: string;
  };
} 