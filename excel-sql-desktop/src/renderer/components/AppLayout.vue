<template>
  <a-layout class="app-layout">
    <!-- 头部导航 -->
    <a-layout-header class="app-header" v-if="showHeader">
      <div class="header-left">
        <div class="logo" v-if="showLogo">
          <slot name="logo">
            <img :src="logoUrl" alt="Logo" class="logo-image" />
            <span class="logo-text">Excel SQL</span>
          </slot>
        </div>
        <slot name="header-left"></slot>
      </div>
      
      <div class="header-center">
        <slot name="header-center"></slot>
      </div>
      
      <div class="header-right">
        <slot name="header-right">
          <a-menu mode="horizontal" :selectedKeys="[currentRoute]" class="header-menu">
            <a-menu-item key="/" @click="$router.push('/')">
              <template #icon><home-outlined /></template>
              首页
            </a-menu-item>
            <a-menu-item key="/workspace" @click="$router.push('/workspace')">
              <template #icon><code-outlined /></template>
              工作区
            </a-menu-item>
            <a-menu-item key="/settings" @click="$router.push('/settings')">
              <template #icon><setting-outlined /></template>
              设置
            </a-menu-item>
          </a-menu>
        </slot>
      </div>
    </a-layout-header>
    
    <!-- 内容区域 -->
    <a-layout class="main-layout">
      <!-- 侧边栏 -->
      <a-layout-sider 
        v-if="showSider" 
        :width="siderWidth" 
        :collapsed="siderCollapsed" 
        :collapsible="collapsible"
        :trigger="null"
        class="app-sider"
        :class="{ 'app-sider-light': siderTheme === 'light' }"
        :theme="siderTheme"
        breakpoint="lg"
        @collapse="handleSiderCollapse"
      >
        <slot name="sider"></slot>
      </a-layout-sider>
      
      <!-- 主内容区 -->
      <a-layout-content class="app-content">
        <div class="content-container responsive-container" :class="{ 'with-padding': contentPadding }">
          <slot></slot>
        </div>
      </a-layout-content>
    </a-layout>
    
    <!-- 页脚 -->
    <a-layout-footer class="app-footer" v-if="showFooter">
      <slot name="footer">
        <div class="footer-content">
          <p>© 2023-2024 Excel SQL Desktop. 保留所有权利。</p>
        </div>
      </slot>
    </a-layout-footer>
  </a-layout>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { HomeOutlined, CodeOutlined, SettingOutlined } from '@ant-design/icons-vue';
import { useAppStore } from '../stores/app';
import logoUrl from '../assets/logo.png';

export default defineComponent({
  name: 'AppLayout',
  components: {
    HomeOutlined,
    CodeOutlined,
    SettingOutlined
  },
  props: {
    showHeader: {
      type: Boolean,
      default: true
    },
    showSider: {
      type: Boolean,
      default: false
    },
    showFooter: {
      type: Boolean,
      default: false
    },
    showLogo: {
      type: Boolean,
      default: true
    },
    siderWidth: {
      type: Number,
      default: 260
    },
    siderCollapsed: {
      type: Boolean,
      default: false
    },
    collapsible: {
      type: Boolean,
      default: true
    },
    contentPadding: {
      type: Boolean,
      default: true
    },
    siderTheme: {
      type: String,
      default: 'dark',
      validator: (value) => ['dark', 'light'].includes(value)
    }
  },
  emits: ['update:siderCollapsed'],
  setup(props, { emit }) {
    const route = useRoute();
    const appStore = useAppStore();
    
    // 当前路由路径
    const currentRoute = computed(() => {
      return route.path;
    });
    
    // 处理侧边栏折叠状态变更
    const handleSiderCollapse = (collapsed) => {
      emit('update:siderCollapsed', collapsed);
    };
    
    // 检测窗口大小变化
    onMounted(() => {
      const handleResize = () => {
        if (window.innerWidth < 768 && !props.siderCollapsed) {
          emit('update:siderCollapsed', true);
        }
      };
      
      window.addEventListener('resize', handleResize);
      handleResize(); // 初始化时执行一次
      
      return () => {
        window.removeEventListener('resize', handleResize);
      };
    });
    
    return {
      currentRoute,
      handleSiderCollapse,
      logoUrl
    };
  }
});
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-layout {
  flex: 1;
  display: flex;
}

.app-header {
  padding: 0 24px;
  height: var(--header-height);
  line-height: var(--header-height);
  background: var(--primary-color);
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--box-shadow-base);
}

.header-left,
.header-right,
.header-center {
  display: flex;
  align-items: center;
  height: 100%;
}

.header-left {
  padding-left: 0;
}

.header-right {
  padding-right: 0;
}

.header-center {
  flex: 1;
  justify-content: center;
}

.logo {
  height: 100%;
  display: flex;
  align-items: center;
  padding-right: 24px;
}

.logo-image {
  height: 36px;
  margin-right: 12px;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
}

.header-menu {
  background: transparent;
  border-bottom: none;
  line-height: var(--header-height);
}

.app-sider {
  box-shadow: var(--box-shadow-base);
  z-index: 9;
  overflow-y: auto;
  overflow-x: hidden;
  height: 100%;
}

.app-sider-light {
  background: var(--component-background);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
}

.app-content {
  position: relative;
  background-color: var(--background-color);
  transition: all 0.3s;
  overflow-x: hidden;
  flex: 1;
}

.content-container {
  min-height: 100%;
  overflow-y: auto;
}

.with-padding {
  padding: 24px;
}

.app-footer {
  padding: 16px 24px;
  text-align: center;
  background: var(--component-background);
  color: var(--text-color-secondary);
  border-top: 1px solid var(--border-color);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .app-header {
    padding: 0 12px;
  }
  
  .logo-text {
    font-size: 16px;
  }
  
  .with-padding {
    padding: 16px;
  }
  
  .app-footer {
    padding: 12px;
  }
}

/* 深色主题适配 */
:global(.dark-theme) .app-footer {
  background: var(--component-background);
  color: var(--text-color-secondary);
  border-top-color: var(--border-color);
}

:global(.dark-theme) .app-sider-light {
  background: var(--component-background);
}
</style> 