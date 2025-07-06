<template>
  <router-view v-slot="{ Component }">
    <transition name="page-transition" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script>
import { defineComponent, onMounted, onBeforeMount, nextTick } from 'vue';
import { useAppStore } from './stores/app';

export default defineComponent({
  name: 'App',
  setup() {
    const appStore = useAppStore();
    
    // 在组件挂载前应用主题，避免闪白
    onBeforeMount(() => {
      appStore.loadSettings();
      appStore.setTheme(appStore.theme);
    });
    
    onMounted(() => {
      // 安全地获取应用信息，避免使用 process
      if (!window.appInfo) {
        window.appInfo = {
          version: '1.0.0',
          electronVersion: '-',
          nodeVersion: '-',
          platform: '-'
        };
      }
      
      // 监听主题变化事件
      if (window.electronAPI?.onThemeChange) {
        window.electronAPI.onThemeChange((event, theme) => {
          appStore.setTheme(theme);
        });
      }
      
      // 确保页面已完全加载
      nextTick(() => {
        // 移除加载动画
        const loadingEl = document.querySelector('.app-loading');
        if (loadingEl) {
          loadingEl.style.opacity = '0';
          setTimeout(() => {
            loadingEl.style.display = 'none';
          }, 300);
        }
      });
    });
    
    return {
      appStore
    };
  }
});
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  overflow: hidden;
  background-color: var(--background-color);
}

#app {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  position: relative;
  background-color: var(--background-color);
  color: var(--text-color);
}

.app-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--background-color);
}

/* 路由切换过渡 */
.page-transition-enter-active,
.page-transition-leave-active {
  transition: opacity 0.2s ease;
}

.page-transition-enter-from,
.page-transition-leave-to {
  opacity: 0;
}

/* 加载动画淡出 */
.app-loading {
  transition: opacity 0.3s ease;
}

/* 避免选中文本 */
.no-select {
  user-select: none;
}

/* 可拖动区域 */
.drag-region {
  -webkit-app-region: drag;
}

/* 非可拖动区域 */
.no-drag {
  -webkit-app-region: no-drag;
}

/* 确保所有滚动容器正常工作 */
.scrollable {
  overflow: auto !important;
  height: 100%;
}

/* 修复Ant Design的滚动问题 */
.ant-layout-content {
  overflow: auto !important;
}

.ant-tabs-content-holder {
  overflow: auto !important;
}

/* 确保表格可以正常滚动 */
.ant-table-body {
  overflow-y: auto !important;
  max-height: 100%;
}
</style> 