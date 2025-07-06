interface ElectronAPI {
  // File operations
  openFile: () => Promise<string | null>;
  saveFile: (options: { defaultPath?: string; filters?: { name: string; extensions: string[] }[] }) => Promise<string | null>;
  readFile: (filePath: string) => Promise<{ success: boolean; data?: string; error?: string }>;
  writeFile: (filePath: string, data: string) => Promise<{ success: boolean; error?: string }>;
  
  // Recent files
  getRecentFiles: () => Promise<{ name: string; path: string; lastOpened: string }[]>;
  saveRecentFiles: (recentFiles: { name: string; path: string; lastOpened: string }[]) => Promise<{ success: boolean; error?: string }>;
  
  // API requests
  apiRequest: (config: any) => Promise<{
    data: any;
    status: number;
    headers: any;
    success: boolean;
    error?: string;
  }>;
}

declare interface Window {
  electronAPI: ElectronAPI;
} 