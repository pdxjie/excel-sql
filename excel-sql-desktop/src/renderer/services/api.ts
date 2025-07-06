import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import {message} from "ant-design-vue";

// Create axios instance with base URL from localStorage or default
const getBaseUrl = () => {
  return localStorage.getItem('apiBaseUrl') || 'http://localhost:8080/api';
};

// 使用 electronAPI 的安全请求
const request = async (config: AxiosRequestConfig): Promise<any> => {
  if (window.electronAPI && window.electronAPI.apiRequest) {
    // 使用 Electron 的安全请求
    const fullConfig = {
      ...config,
      baseURL: config.baseURL || getBaseUrl(),
      headers: {
        'Content-Type': 'application/json',
        ...config.headers
      }
    };
    
    try {
      const response = await window.electronAPI.apiRequest(fullConfig);
      if (!response.success) {
        message.error(response.data.errorMessage)
        throw new Error(response.error || 'API request failed');
      }
      
      // 确保返回正确的数据格式
      return {
        data: response.data,
        status: response.status,
        headers: response.headers
      };
    } catch (error: any) {
      console.error('API request error:', error);
      throw error;
    }
  } else {
    // 回退到直接使用 axios（开发模式）
    const instance = axios.create({
      baseURL: getBaseUrl(),
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    });
    return instance(config);
  }
};

// API 服务
const api = {
  get: (url: string, config?: AxiosRequestConfig) => 
    request({ method: 'get', url, ...config })
      .then(res => {
        console.log(`API GET ${url} response:`, res);
        return res.data;
      }),
  
  post: (url: string, data?: any, config?: AxiosRequestConfig) => 
    request({ method: 'post', url, data, ...config })
      .then(res => {
        console.log(`API POST ${url} response:`, res);
        return res.data;
      }),
  
  put: (url: string, data?: any, config?: AxiosRequestConfig) => 
    request({ method: 'put', url, data, ...config })
      .then(res => {
        console.log(`API PUT ${url} response:`, res);
        return res.data;
      }),
  
  delete: (url: string, config?: AxiosRequestConfig) => 
    request({ method: 'delete', url, ...config })
      .then(res => {
        console.log(`API DELETE ${url} response:`, res);
        return res.data;
      })
};

// Update baseURL when it changes in localStorage
window.addEventListener('storage', (event) => {
  if (event.key === 'apiBaseUrl' && event.newValue) {
    console.log('API base URL updated:', event.newValue);
  }
});

export default api; 