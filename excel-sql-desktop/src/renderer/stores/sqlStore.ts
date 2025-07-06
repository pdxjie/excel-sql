import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { sqlService, Connection, ExcelFile } from '../services';

export const useSqlStore = defineStore('sql', () => {
  // State
  const connections = ref<Connection[]>([]);
  const excelFiles = ref<ExcelFile[]>([]);
  const currentConnectionId = ref<string | null>(null);
  const currentFileId = ref<string | null>(null);
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  // Getters
  const currentConnection = computed(() => {
    if (!currentConnectionId.value) return null;
    return connections.value.find((conn: Connection) => conn.id === currentConnectionId.value) || null;
  });

  const currentFile = computed(() => {
    if (!currentFileId.value) return null;
    return excelFiles.value.find(file => file.name === currentFileId.value) || null;
  });

  // Actions
  async function fetchConnections() {
    isLoading.value = true;
    error.value = null;
    
    try {
      connections.value = await sqlService.getConnections();
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch connections';
      console.error('Error fetching connections:', err);
    } finally {
      isLoading.value = false;
    }
  }

  async function fetchFiles() {
    isLoading.value = true;
    error.value = null;
    
    try {
      // 尝试从API获取文件
      excelFiles.value = await sqlService.getFiles();
      console.log('Excel files:', excelFiles.value)
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch Excel files';
      console.error('Error fetching Excel files:', err);
      
      // 如果API不可用，使用空数组
      excelFiles.value = [];
    } finally {
      isLoading.value = false;
    }
  }

  async function addConnection(connection: Omit<Connection, 'id'>) {
    isLoading.value = true;
    error.value = null;
    
    try {
      const newConnection = await sqlService.createConnection(connection);
      connections.value.push(newConnection);
      return newConnection;
    } catch (err: any) {
      error.value = err.message || 'Failed to add connection';
      console.error('Error adding connection:', err);
      throw err;
    } finally {
      isLoading.value = false;
    }
  }

  function removeConnection(connectionId: string) {
    // Remove from local state
    connections.value = connections.value.filter(conn => conn.id !== connectionId);
    
    // If this was the current connection, clear it
    if (currentConnectionId.value === connectionId) {
      currentConnectionId.value = null;
    }
    
    // Note: We're not actually deleting from the backend here
    // In a real app, you might want to call an API to delete the connection
    // sqlService.deleteConnection(connectionId);
  }

  async function testConnection(connection: Omit<Connection, 'id'>) {
    isLoading.value = true;
    error.value = null;
    
    try {
      return await sqlService.testConnection(connection);
    } catch (err: any) {
      error.value = err.message || 'Failed to test connection';
      console.error('Error testing connection:', err);
      throw err;
    } finally {
      isLoading.value = false;
    }
  }

  async function executeQuery(query: { sql: string; connectionId?: string; filePath?: string; params?: any[] }) {
    isLoading.value = true;
    error.value = null;
    try {
      return await sqlService.executeQuery(query);
    } catch (err: any) {
      error.value = err.message || 'Failed to execute query';
      console.error('Error executing query:', err);
      
      // 返回空数据结构
      return {
        columns: null,
        rows: null,
        affectedRows: null,
        executionTime: 0,
        success: false,
        errorMessage: err.message || 'Failed to execute query',
        sqlType: null
      };
    } finally {
      isLoading.value = false;
    }
  }

  function setCurrentConnection(connectionId: string) {
    currentConnectionId.value = connectionId;
  }

  function setCurrentFile(fileName: string) {
    currentFileId.value = fileName;
  }

  return {
    // State
    connections,
    excelFiles,
    currentConnectionId,
    currentFileId,
    isLoading,
    error,
    
    // Getters
    currentConnection,
    currentFile,
    
    // Actions
    fetchConnections,
    fetchFiles,
    addConnection,
    removeConnection,
    testConnection,
    executeQuery,
    setCurrentConnection,
    setCurrentFile
  };
}); 