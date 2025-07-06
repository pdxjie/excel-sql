<template>
  <div class="query-editor">
    <div class="editor-header">
      <div class="query-info">
        <div class="query-name" v-if="query.name">
          <code-outlined />
          <span>{{ query.name }}</span>
        </div>
        <div class="query-database" v-if="databaseName">
          <database-outlined />
          <span>{{ databaseName }}</span>
        </div>
      </div>
      <div class="editor-actions">
        <a-tooltip title="运行查询">
          <a-button type="primary" @click="runQuery" :loading="loading">
            <template #icon><play-circle-outlined /></template>
            运行
          </a-button>
        </a-tooltip>
        <a-tooltip title="保存查询">
          <a-button @click="saveQuery" :disabled="loading">
            <template #icon><save-outlined /></template>
            保存
          </a-button>
        </a-tooltip>
        <a-tooltip title="格式化 SQL">
          <a-button @click="formatSql" :disabled="loading">
            <template #icon><align-left-outlined /></template>
          </a-button>
        </a-tooltip>
      </div>
    </div>
    
    <div class="editor-container">
      <div ref="monacoContainer" class="monaco-container"></div>
    </div>
    
    <a-modal v-model:open="saveModalVisible" title="保存查询" @ok="handleSaveQuery">
      <a-form :model="saveForm" layout="vertical">
        <a-form-item label="查询名称" name="name">
          <a-input v-model:value="saveForm.name" placeholder="输入查询名称" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="saveForm.description" placeholder="输入查询描述（可选）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, onBeforeUnmount, watch, computed, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import * as monaco from 'monaco-editor';
import { format } from 'sql-formatter';
import {
  CodeOutlined,
  DatabaseOutlined,
  PlayCircleOutlined,
  SaveOutlined,
  AlignLeftOutlined
} from '@ant-design/icons-vue';
import { sqlService } from '../services';

// 确保Monaco编辑器只加载一次
let monacoLoaded = false;

export default defineComponent({
  name: 'QueryEditor',
  components: {
    CodeOutlined,
    DatabaseOutlined,
    PlayCircleOutlined,
    SaveOutlined,
    AlignLeftOutlined
  },
  props: {
    databaseId: {
      type: String,
      required: true
    },
    databaseName: {
      type: String,
      default: ''
    },
    initialQuery: {
      type: Object,
      default: () => ({
        id: 'new',
        name: '',
        description: '',
        sql: ''
      })
    }
  },
  emits: ['run-query', 'save-query'],
  setup(props, { emit }) {
    const monacoContainer = ref(null);
    let editor = null;
    const loading = ref(false);
    const saveModalVisible = ref(false);
    const editorReady = ref(false);
    
    // 查询信息
    const query = reactive({
      id: props.initialQuery.id || 'new',
      name: props.initialQuery.name || '',
      description: props.initialQuery.description || '',
      sql: props.initialQuery.sql || ''
    });
    
    // 保存表单
    const saveForm = reactive({
      name: '',
      description: ''
    });
    
    // 初始化编辑器
    const initEditor = async () => {
      if (!monacoContainer.value) return;
      
      try {
        // 确保容器已渲染
        await nextTick();
        
        // 注册 SQL 语言
        if (!monacoLoaded) {
          monaco.languages.register({ id: 'sql' });
          monacoLoaded = true;
        }
        
        // 创建编辑器
        editor = monaco.editor.create(monacoContainer.value, {
          value: query.sql,
          language: 'sql',
          theme: 'vs-dark',
          automaticLayout: true,
          minimap: {
            enabled: true
          },
          scrollBeyondLastLine: false,
          fontSize: 14,
          tabSize: 2,
          lineNumbers: 'on',
          roundedSelection: true,
          scrollbar: {
            useShadows: false,
            verticalHasArrows: true,
            horizontalHasArrows: true,
            vertical: 'visible',
            horizontal: 'visible',
            verticalScrollbarSize: 12,
            horizontalScrollbarSize: 12
          }
        });
        
        // 监听内容变化
        editor.onDidChangeModelContent(() => {
          query.sql = editor.getValue();
        });
        
        // 设置编辑器就绪状态
        editorReady.value = true;
        
        // 确保编辑器正确布局
        window.addEventListener('resize', () => {
          if (editor) {
            editor.layout();
          }
        });
      } catch (error) {
        console.error('初始化编辑器失败:', error);
        message.error('初始化编辑器失败');
      }
    };
    
    // 运行查询
    const runQuery = () => {
      if (!editor || !editorReady.value) {
        message.warning('编辑器尚未就绪');
        return;
      }
      
      const sql = editor.getValue().trim();
      if (!sql) {
        message.warning('请输入 SQL 查询');
        return;
      }
      
      loading.value = true;
      
      // 触发运行查询事件
      emit('run-query', {
        databaseId: props.databaseId,
        sql: sql,
        queryId: query.id,
        name: query.name
      });
      
      // 模拟查询执行
      setTimeout(() => {
        loading.value = false;
      }, 1000);
    };
    
    // 保存查询
    const saveQuery = () => {
      if (!editor || !editorReady.value) {
        message.warning('编辑器尚未就绪');
        return;
      }
      
      saveForm.name = query.name || '';
      saveForm.description = query.description || '';
      saveModalVisible.value = true;
    };
    
    // 处理保存查询
    const handleSaveQuery = () => {
      if (!saveForm.name) {
        message.error('请输入查询名称');
        return;
      }
      
      if (!editor || !editorReady.value) {
        message.warning('编辑器尚未就绪');
        return;
      }
      
      const sql = editor.getValue().trim();
      if (!sql) {
        message.warning('请输入 SQL 查询');
        return;
      }
      
      // 更新查询信息
      query.name = saveForm.name;
      query.description = saveForm.description;
      query.sql = sql;
      
      // 触发保存查询事件
      emit('save-query', {
        databaseId: props.databaseId,
        queryId: query.id,
        name: query.name,
        description: query.description,
        sql: sql
      });
      
      saveModalVisible.value = false;
      message.success('查询已保存');
    };
    
    // 格式化 SQL
    const formatSql = () => {
      if (!editor || !editorReady.value) {
        message.warning('编辑器尚未就绪');
        return;
      }
      
      const sql = editor.getValue();
      
      if (!sql.trim()) {
        return;
      }
      
      try {
        // 使用sql-formatter库格式化SQL
        const formattedSql = format(sql, {
          language: 'sql',
          uppercase: true,
          linesBetweenQueries: 2,
          indentStyle: 'standard'
        });
        
        editor.setValue(formattedSql);
        message.success('SQL 已格式化');
      } catch (error) {
        console.error('SQL 格式化失败:', error);
        message.error('SQL 格式化失败');
      }
    };
    
    // 监听 props 变化
    watch(() => props.initialQuery, (newVal) => {
      if (newVal && editor && editorReady.value) {
        query.id = newVal.id || 'new';
        query.name = newVal.name || '';
        query.description = newVal.description || '';
        query.sql = newVal.sql || '';
        editor.setValue(query.sql);
      }
    }, { deep: true });
    
    // 组件挂载时初始化编辑器
    onMounted(() => {
      // 使用nextTick确保DOM已经渲染
      nextTick(() => {
        initEditor();
      });
    });
    
    // 组件销毁时销毁编辑器
    onBeforeUnmount(() => {
      if (editor) {
        editor.dispose();
        editor = null;
      }
    });
    
    return {
      monacoContainer,
      loading,
      query,
      saveModalVisible,
      saveForm,
      editorReady,
      runQuery,
      saveQuery,
      handleSaveQuery,
      formatSql
    };
  }
});
</script>

<style scoped>
.query-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--component-background);
}

.editor-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.query-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.query-name, .query-database {
  display: flex;
  align-items: center;
  gap: 6px;
}

.query-name .anticon, .query-database .anticon {
  color: var(--primary-color);
}

.editor-actions {
  display: flex;
  gap: 8px;
}

.editor-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  min-height: 0; /* 确保滚动正常工作 */
}

.monaco-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style> 