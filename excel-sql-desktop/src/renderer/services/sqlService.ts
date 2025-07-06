import api from './api';

export interface QueryRequest {
  sql: string;
  connectionId?: string;
  filePath?: string;
  params?: any[];
  useCache?: boolean;
}

export interface QueryColumn {
  name: string;
  label: string;
  dataType: string;
  aggregated: boolean;
}

export interface QueryResponse {
  columns: QueryColumn[] | null;
  rows: any[] | null;
  affectedRows?: number | null;
  executionTime?: number;
  success: boolean;
  errorMessage?: string | null;
  sqlType?: string;
}

export interface Connection {
  id: string;
  name: string;
  type: string;
  host: string;
  port: number;
  username: string;
  database?: string;
}

export interface ExcelFile {
  name: string;
  filePath: string;
  fileSize: number;
  lastModified: string;
  sheets: string[];
}

const sqlService = {
  /**
   * Get list of Excel files
   */
  async getFiles(): Promise<ExcelFile[]> {
    try {
      const response = await api.get('/storage/files');
      console.log('getFiles response:', response);
      
      // 如果响应是数组，直接返回
      if (Array.isArray(response)) {
        return response;
      }
      
      // 如果响应包含 data 字段且是数组，返回 data
      if (response && Array.isArray(response.data)) {
        return response.data;
      }
      
      // 如果响应不是预期格式，返回空数组
      console.warn('Unexpected response format from getFiles:', response);
      return [];
    } catch (error) {
      console.error('Error in getFiles:', error);
      throw error;
    }
  },

  /**
   * Execute SQL query
   */
  async executeQuery(request: QueryRequest): Promise<QueryResponse> {
    try {
      // 设置默认值
      const queryRequest = {
        ...request,
        useCache: false
      };
      
      const response = await api.post('/sql/query', queryRequest);
      console.log('executeQuery response:', response);
      
      if (response && typeof response === 'object') {
        // 处理不同类型的 SQL 响应
        const sqlResponse: QueryResponse = {
          columns: response.columns || null,
          rows: response.rows || null,
          affectedRows: response.affectedRows,
          executionTime: response.executionTime,
          success: response.success !== undefined ? response.success : true,
          errorMessage: response.errorMessage || null,
          sqlType: response.sqlType
        };
        
        return sqlResponse;
      }
      
      throw new Error('Invalid response format from query execution');
    } catch (error) {
      console.error('Error in executeQuery:', error);
      throw error;
    }
  },

  /**
   * Get list of connections
   */
  async getConnections(): Promise<Connection[]> {
    try {
      const response = await api.get('/connections');
      console.log('getConnections response:', response);
      
      if (Array.isArray(response)) {
        return response;
      }
      
      if (response && Array.isArray(response.data)) {
        return response.data;
      }
      
      console.warn('Unexpected response format from getConnections:', response);
      return [];
    } catch (error) {
      console.error('Error in getConnections:', error);
      throw error;
    }
  },

  /**
   * Create a new connection
   */
  createConnection(connection: Omit<Connection, 'id'>): Promise<Connection> {
    return api.post('/connections', connection);
  },

  /**
   * Test a connection
   */
  testConnection(connection: Omit<Connection, 'id'>): Promise<{ success: boolean; message?: string }> {
    return api.post('/connections/test', connection);
  },

  /**
   * Get database schema
   */
  getDatabaseSchema(connectionId: string): Promise<any> {
    return api.get(`/connections/${connectionId}/schema`);
  }
};

export default sqlService; 