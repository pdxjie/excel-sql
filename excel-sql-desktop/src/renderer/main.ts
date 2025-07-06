import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import './assets/main.css';
import './styles/theme.css';

// 创建应用实例
const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(Antd);

// 确保主题在挂载前应用
const applyTheme = () => {
  const theme = localStorage.getItem('theme') || 'light';
  const primaryColor = localStorage.getItem('primaryColor') || 'blue';
  
  // 应用主题
  if (theme === 'dark' || (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.classList.add('dark-theme');
    document.documentElement.style.backgroundColor = '#121212';
  } else {
    document.documentElement.classList.remove('dark-theme');
    document.documentElement.style.backgroundColor = '#f8f9fa';
  }
  
  // 应用主色调
  const colorMap: Record<string, string> = {
    blue: '#1890ff',
    green: '#52c41a',
    purple: '#722ed1',
    red: '#f5222d',
    orange: '#fa8c16',
    cyan: '#13c2c2'
  };
  
  if (colorMap[primaryColor]) {
    document.documentElement.style.setProperty('--primary-color', colorMap[primaryColor]);
  }
};

// 应用主题
applyTheme();

// 挂载应用
app.mount('#app');

// 移除加载状态
document.documentElement.classList.add('theme-ready'); 