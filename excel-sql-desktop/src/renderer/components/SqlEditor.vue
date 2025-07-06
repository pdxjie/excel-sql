<template>
  <div class="sql-editor-container" :class="{ 'full-height': fullHeight }">
    <div class="editor-toolbar" v-if="showToolbar">
      <div class="toolbar-left">
        <a-button-group>
          <a-button 
            type="primary" 
            @click="executeQuery" 
            :loading="loading"
            title="执行查询 (Ctrl+Enter)"
          >
            <template #icon><play-circle-outlined /></template>
            执行
          </a-button>
          <a-button 
            @click="formatSQL" 
            title="格式化 SQL (Ctrl+Shift+F)"
          >
            <template #icon><align-left-outlined /></template>
            格式化
          </a-button>
        </a-button-group>
        
        <a-button 
          @click="clearEditor" 
          class="ml-1"
          title="清空编辑器"
        >
          <template #icon><delete-outlined /></template>
          清空
        </a-button>
      </div>
      
      <div class="toolbar-right">
        <a-select 
          v-model:value="editorTheme" 
          style="width: 120px" 
          size="small"
          @change="updateEditorOptions"
        >
          <a-select-option value="vs">浅色主题</a-select-option>
          <a-select-option value="vs-dark">深色主题</a-select-option>
          <a-select-option value="hc-black">高对比度</a-select-option>
        </a-select>
        
        <a-tooltip title="保存查询">
          <a-button type="text" @click="saveQuery">
            <template #icon><save-outlined /></template>
          </a-button>
        </a-tooltip>
        
        <a-tooltip title="加载查询">
          <a-button type="text" @click="loadQuery">
            <template #icon><folder-open-outlined /></template>
          </a-button>
        </a-tooltip>
      </div>
    </div>
    
    <div ref="monacoContainer" class="monaco-container"></div>
    
    <div class="editor-statusbar" v-if="showStatusBar">
      <div class="statusbar-left">
        <span>{{ cursorPosition }}</span>
      </div>
      <div class="statusbar-right">
        <span>{{ editorLanguage }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { message } from 'ant-design-vue';
import { 
  PlayCircleOutlined, 
  AlignLeftOutlined, 
  DeleteOutlined, 
  SaveOutlined, 
  FolderOpenOutlined 
} from '@ant-design/icons-vue';

// 默认的 SQL 查询模板
const DEFAULT_SQL = `-- 在此输入 SQL 查询
SELECT * FROM Sheet1 LIMIT 100;
`;

export default defineComponent({
  name: 'SqlEditor',
  components: {
    PlayCircleOutlined,
    AlignLeftOutlined,
    DeleteOutlined,
    SaveOutlined,
    FolderOpenOutlined
  },
  props: {
    value: {
      type: String,
      default: DEFAULT_SQL
    },
    language: {
      type: String,
      default: 'sql'
    },
    theme: {
      type: String,
      default: 'vs-dark'
    },
    readOnly: {
      type: Boolean,
      default: false
    },
    minimap: {
      type: Boolean,
      default: true
    },
    lineNumbers: {
      type: Boolean,
      default: true
    },
    wordWrap: {
      type: Boolean,
      default: true
    },
    showToolbar: {
      type: Boolean,
      default: true
    },
    showStatusBar: {
      type: Boolean,
      default: true
    },
    fullHeight: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:value', 'execute', 'save', 'load', 'format', 'clear'],
  setup(props, { emit }) {
    // 引用和状态
    const monacoContainer = ref(null);
    const editor = ref(null);
    const monaco = ref(null);
    const loading = ref(false);
    const editorTheme = ref(props.theme);
    const editorLanguage = ref(props.language);
    const cursorPosition = ref('行 1, 列 1');
    
    // 监听值变化
    watch(() => props.value, (newValue) => {
      if (editor.value && newValue !== editor.value.getValue()) {
        editor.value.setValue(newValue);
      }
    });
    
    // 监听主题变化
    watch(() => props.theme, (newTheme) => {
      editorTheme.value = newTheme;
      if (monaco.value) {
        monaco.value.editor.setTheme(newTheme);
      }
    });
    
    // 初始化编辑器
    const initMonaco = async () => {
      if (!monacoContainer.value) return;
      
      try {
        // 动态导入 Monaco 编辑器
        const monacoEditor = await import('monaco-editor');
        monaco.value = monacoEditor;
        
        // 创建编辑器实例
        editor.value = monacoEditor.editor.create(monacoContainer.value, {
          value: props.value,
          language: props.language,
          theme: editorTheme.value,
          automaticLayout: true,
          minimap: {
            enabled: props.minimap
          },
          lineNumbers: props.lineNumbers ? 'on' : 'off',
          readOnly: props.readOnly,
          wordWrap: props.wordWrap ? 'on' : 'off',
          scrollBeyondLastLine: false,
          fontSize: 14,
          tabSize: 2,
          folding: true,
          renderLineHighlight: 'all',
          suggestOnTriggerCharacters: true,
          snippetSuggestions: 'inline',
          contextmenu: true,
          scrollbar: {
            useShadows: false,
            verticalHasArrows: false,
            horizontalHasArrows: false,
            vertical: 'auto',
            horizontal: 'auto'
          }
        });
        
        // 添加内容变更事件
        editor.value.onDidChangeModelContent(() => {
          const value = editor.value.getValue();
          emit('update:value', value);
        });
        
        // 添加光标位置变更事件
        editor.value.onDidChangeCursorPosition((e) => {
          cursorPosition.value = `行 ${e.position.lineNumber}, 列 ${e.position.column}`;
        });
        
        // 添加键盘快捷键
        editor.value.addCommand(monacoEditor.KeyMod.CtrlCmd | monacoEditor.KeyCode.Enter, () => {
          executeQuery();
        });
        
        editor.value.addCommand(
          monacoEditor.KeyMod.CtrlCmd | monacoEditor.KeyMod.Shift | monacoEditor.KeyCode.KeyF, 
          () => {
            formatSQL();
          }
        );
        
        editor.value.addCommand(monacoEditor.KeyMod.CtrlCmd | monacoEditor.KeyCode.KeyS, () => {
          saveQuery();
        });
        
      } catch (error) {
        console.error('初始化编辑器失败:', error);
        message.error('初始化编辑器失败');
      }
    };
    
    // 更新编辑器选项
    const updateEditorOptions = () => {
      if (!editor.value || !monaco.value) return;
      
      monaco.value.editor.setTheme(editorTheme.value);
      
      editor.value.updateOptions({
        minimap: {
          enabled: props.minimap
        },
        lineNumbers: props.lineNumbers ? 'on' : 'off',
        readOnly: props.readOnly,
        wordWrap: props.wordWrap ? 'on' : 'off'
      });
    };
    
    // 执行查询
    const executeQuery = () => {
      if (!editor.value) return;
      
      const sql = editor.value.getValue();
      if (!sql.trim()) {
        message.warning('请输入 SQL 查询');
        return;
      }
      
      loading.value = true;
      emit('execute', sql);
      
      // 模拟异步操作
      setTimeout(() => {
        loading.value = false;
      }, 500);
    };
    
    // 格式化 SQL
    const formatSQL = () => {
      if (!editor.value || !monaco.value) return;
      
      editor.value.getAction('editor.action.formatDocument').run();
      emit('format');
    };
    
    // 清空编辑器
    const clearEditor = () => {
      if (!editor.value) return;
      
      editor.value.setValue('');
      emit('clear');
    };
    
    // 保存查询
    const saveQuery = () => {
      if (!editor.value) return;
      
      const sql = editor.value.getValue();
      emit('save', sql);
    };
    
    // 加载查询
    const loadQuery = () => {
      emit('load');
    };
    
    // 设置编辑器内容
    const setValue = (value) => {
      if (editor.value) {
        editor.value.setValue(value);
      }
    };
    
    // 获取编辑器内容
    const getValue = () => {
      if (editor.value) {
        return editor.value.getValue();
      }
      return '';
    };
    
    // 聚焦编辑器
    const focus = () => {
      if (editor.value) {
        editor.value.focus();
      }
    };
    
    // 组件挂载时初始化编辑器
    onMounted(() => {
      initMonaco();
    });
    
    // 组件卸载前销毁编辑器
    onBeforeUnmount(() => {
      if (editor.value) {
        editor.value.dispose();
      }
    });
    
    return {
      monacoContainer,
      loading,
      editorTheme,
      editorLanguage,
      cursorPosition,
      executeQuery,
      formatSQL,
      clearEditor,
      saveQuery,
      loadQuery,
      updateEditorOptions,
      setValue,
      getValue,
      focus
    };
  }
});
</script>

<style scoped>
.sql-editor-container {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-base);
  overflow: hidden;
  background-color: var(--component-background);
}

.full-height {
  height: 100%;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--component-background);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.monaco-container {
  flex: 1;
  min-height: 200px;
  overflow: hidden;
}

.editor-statusbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  font-size: 12px;
  background-color: var(--component-background);
  border-top: 1px solid var(--border-color);
  color: var(--text-color-secondary);
}

.statusbar-left,
.statusbar-right {
  display: flex;
  align-items: center;
}

/* 深色主题适配 */
:global(.dark-theme) .sql-editor-container {
  border-color: #303030;
}

:global(.dark-theme) .editor-toolbar,
:global(.dark-theme) .editor-statusbar {
  background-color: #1f1f1f;
  border-color: #303030;
}
</style> 