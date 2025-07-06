<template>
  <transition
    :name="name"
    :mode="mode"
    :appear="appear"
    @before-enter="beforeEnter"
    @enter="enter"
    @after-enter="afterEnter"
    @before-leave="beforeLeave"
    @leave="leave"
    @after-leave="afterLeave"
  >
    <slot></slot>
  </transition>
</template>

<script>
import { defineComponent, onMounted, inject } from 'vue';
import { useAppStore } from '../stores/app';

export default defineComponent({
  name: 'PageTransition',
  props: {
    name: {
      type: String,
      default: 'page'
    },
    mode: {
      type: String,
      default: 'out-in'
    },
    appear: {
      type: Boolean,
      default: true
    }
  },
  emits: ['before-enter', 'enter', 'after-enter', 'before-leave', 'leave', 'after-leave'],
  setup(props, { emit }) {
    const appStore = useAppStore();
    
    onMounted(() => {
      // 确保页面加载时应用正确的主题
      const theme = appStore.theme;
      if (theme === 'dark') {
        document.documentElement.classList.add('dark-theme');
      } else if (theme === 'light') {
        document.documentElement.classList.remove('dark-theme');
      } else if (theme === 'system') {
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (prefersDark) {
          document.documentElement.classList.add('dark-theme');
        } else {
          document.documentElement.classList.remove('dark-theme');
        }
      }
    });
    
    const beforeEnter = (el) => {
      // 确保元素在进入前具有正确的背景色
      el.style.backgroundColor = 'var(--background-color)';
      emit('before-enter', el);
    };
    
    const enter = (el, done) => {
      emit('enter', el, done);
      done();
    };
    
    const afterEnter = (el) => {
      emit('after-enter', el);
    };
    
    const beforeLeave = (el) => {
      // 确保元素在离开前具有正确的背景色
      el.style.backgroundColor = 'var(--background-color)';
      emit('before-leave', el);
    };
    
    const leave = (el, done) => {
      emit('leave', el, done);
      done();
    };
    
    const afterLeave = (el) => {
      emit('after-leave', el);
    };
    
    return {
      beforeEnter,
      enter,
      afterEnter,
      beforeLeave,
      leave,
      afterLeave
    };
  }
});
</script>

<style scoped>
/* 默认过渡效果 */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.3s, transform 0.3s;
  background-color: var(--background-color);
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(10px);
  background-color: var(--background-color);
}

/* 淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
  background-color: var(--background-color);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  background-color: var(--background-color);
}

/* 滑动效果 */
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.3s, opacity 0.3s;
  background-color: var(--background-color);
}

.slide-left-enter-from,
.slide-right-leave-to {
  opacity: 0;
  transform: translateX(30px);
  background-color: var(--background-color);
}

.slide-left-leave-to,
.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-30px);
  background-color: var(--background-color);
}

/* 缩放效果 */
.zoom-enter-active,
.zoom-leave-active {
  transition: transform 0.3s, opacity 0.3s;
  background-color: var(--background-color);
}

.zoom-enter-from,
.zoom-leave-to {
  opacity: 0;
  transform: scale(0.95);
  background-color: var(--background-color);
}

/* 从底部滑入 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s, opacity 0.3s;
  background-color: var(--background-color);
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(30px);
  background-color: var(--background-color);
}

/* 从顶部滑入 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: transform 0.3s, opacity 0.3s;
  background-color: var(--background-color);
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-30px);
  background-color: var(--background-color);
}
</style> 