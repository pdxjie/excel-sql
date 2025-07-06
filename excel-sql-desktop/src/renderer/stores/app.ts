import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAppStore = defineStore('app', () => {
  // State
  const theme = ref<'light' | 'dark' | 'system'>('system');
  const primaryColor = ref<string>('blue');
  const apiBaseUrl = ref<string>('http://localhost:8080/api');

  // Actions
  function loadSettings() {
    // Load theme
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme && ['light', 'dark', 'system'].includes(savedTheme)) {
      theme.value = savedTheme as 'light' | 'dark' | 'system';
    }

    // Load primary color
    const savedColor = localStorage.getItem('primaryColor');
    if (savedColor) {
      primaryColor.value = savedColor;
    }

    // Load API URL
    const savedApiUrl = localStorage.getItem('apiBaseUrl');
    if (savedApiUrl) {
      apiBaseUrl.value = savedApiUrl;
    }
  }

  function setTheme(newTheme: 'light' | 'dark' | 'system') {
    theme.value = newTheme;
    localStorage.setItem('theme', newTheme);

    // Apply theme to document
    if (newTheme === 'dark' || (newTheme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
      document.documentElement.classList.add('dark-theme');
      document.documentElement.style.backgroundColor = '#121212';
      document.body.style.backgroundColor = '#121212';
    } else {
      document.documentElement.classList.remove('dark-theme');
      document.documentElement.style.backgroundColor = '#f8f9fa';
      document.body.style.backgroundColor = '#f8f9fa';
    }
  }

  function setPrimaryColor(color: string) {
    primaryColor.value = color;
    localStorage.setItem('primaryColor', color);

    // Apply color to document
    const colorMap: Record<string, string> = {
      blue: '#1890ff',
      green: '#52c41a',
      purple: '#722ed1',
      red: '#f5222d',
      orange: '#fa8c16',
      cyan: '#13c2c2'
    };
    
    if (colorMap[color]) {
      document.documentElement.style.setProperty('--primary-color', colorMap[color]);
    }
  }

  function setApiBaseUrl(url: string) {
    apiBaseUrl.value = url;
    localStorage.setItem('apiBaseUrl', url);
  }

  return {
    // State
    theme,
    primaryColor,
    apiBaseUrl,
    
    // Actions
    loadSettings,
    setTheme,
    setPrimaryColor,
    setApiBaseUrl
  };
}); 