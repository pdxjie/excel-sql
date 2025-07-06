<template>
  <div class="data-table-container" :class="{ 'full-height': fullHeight }">
    <div class="table-toolbar" v-if="showToolbar">
      <div class="toolbar-left">
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索数据..."
          style="width: 250px"
          @search="onSearch"
          allowClear
        />
        
        <a-select 
          v-model:value="pageSize" 
          style="width: 120px; margin-left: 8px;" 
          @change="onPageSizeChange"
        >
          <a-select-option :value="10">10 条/页</a-select-option>
          <a-select-option :value="20">20 条/页</a-select-option>
          <a-select-option :value="50">50 条/页</a-select-option>
          <a-select-option :value="100">100 条/页</a-select-option>
        </a-select>
      </div>
      
      <div class="toolbar-right">
        <a-button-group>
          <a-tooltip title="导出为 Excel">
            <a-button @click="exportData('excel')">
              <template #icon><file-excel-outlined /></template>
              Excel
            </a-button>
          </a-tooltip>
          <a-tooltip title="导出为 CSV">
            <a-button @click="exportData('csv')">
              <template #icon><file-text-outlined /></template>
              CSV
            </a-button>
          </a-tooltip>
          <a-tooltip title="导出为 JSON">
            <a-button @click="exportData('json')">
              <template #icon><code-outlined /></template>
              JSON
            </a-button>
          </a-tooltip>
        </a-button-group>
        
        <a-tooltip title="刷新数据">
          <a-button type="text" @click="refreshData">
            <template #icon><reload-outlined /></template>
          </a-button>
        </a-tooltip>
      </div>
    </div>
    
    <div class="table-wrapper">
      <a-spin :spinning="loading" tip="加载中...">
        <a-table
          :columns="columns"
          :data-source="filteredData"
          :pagination="pagination"
          :scroll="{ x: true, y: tableHeight }"
          :bordered="bordered"
          size="middle"
          @change="handleTableChange"
        >
          <!-- 自定义表头 -->
          <template #headerCell="{ column }">
            <span :title="column.title">{{ column.title }}</span>
            <a-dropdown v-if="column.dataIndex !== 'operation'">
              <template #overlay>
                <a-menu>
                  <a-menu-item key="sort-asc" @click="sortColumn(column.dataIndex, 'asc')">
                    <sort-ascending-outlined /> 升序
                  </a-menu-item>
                  <a-menu-item key="sort-desc" @click="sortColumn(column.dataIndex, 'desc')">
                    <sort-descending-outlined /> 降序
                  </a-menu-item>
                  <a-menu-item key="filter" @click="showFilterModal(column)">
                    <filter-outlined /> 筛选
                  </a-menu-item>
                </a-menu>
              </template>
              <down-outlined class="column-menu-trigger" />
            </a-dropdown>
          </template>
        </a-table>
      </a-spin>
    </div>
    
    <div class="table-footer" v-if="showFooter">
      <div class="footer-left">
        <span>共 {{ totalRecords }} 条记录</span>
      </div>
      <div class="footer-right">
        <a-button size="small" @click="copyToClipboard">
          <template #icon><copy-outlined /></template>
          复制数据
        </a-button>
      </div>
    </div>
    
    <!-- 筛选弹窗 -->
    <a-modal
      v-model:open="filterModalVisible"
      title="数据筛选"
      @ok="applyFilter"
      @cancel="cancelFilter"
      :maskClosable="false"
    >
      <a-form layout="vertical">
        <a-form-item label="筛选列">
          <a-input disabled :value="currentFilterColumn?.title" />
        </a-form-item>
        <a-form-item label="筛选条件">
          <a-select v-model:value="filterCondition" style="width: 100%">
            <a-select-option value="equals">等于</a-select-option>
            <a-select-option value="notEquals">不等于</a-select-option>
            <a-select-option value="contains">包含</a-select-option>
            <a-select-option value="notContains">不包含</a-select-option>
            <a-select-option value="startsWith">开头是</a-select-option>
            <a-select-option value="endsWith">结尾是</a-select-option>
            <a-select-option value="greaterThan">大于</a-select-option>
            <a-select-option value="lessThan">小于</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="筛选值">
          <a-input v-model:value="filterValue" placeholder="请输入筛选值" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, computed, watch, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { 
  FileExcelOutlined, 
  FileTextOutlined, 
  CodeOutlined, 
  ReloadOutlined, 
  CopyOutlined,
  SortAscendingOutlined,
  SortDescendingOutlined,
  FilterOutlined,
  DownOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'DataTable',
  components: {
    FileExcelOutlined,
    FileTextOutlined,
    CodeOutlined,
    ReloadOutlined,
    CopyOutlined,
    SortAscendingOutlined,
    SortDescendingOutlined,
    FilterOutlined,
    DownOutlined
  },
  props: {
    data: {
      type: Array,
      default: () => []
    },
    columns: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    bordered: {
      type: Boolean,
      default: true
    },
    showToolbar: {
      type: Boolean,
      default: true
    },
    showFooter: {
      type: Boolean,
      default: true
    },
    fullHeight: {
      type: Boolean,
      default: false
    }
  },
  emits: ['refresh', 'export', 'search'],
  setup(props, { emit }) {
    // 状态
    const searchText = ref('');
    const pageSize = ref(20);
    const currentPage = ref(1);
    const tableHeight = ref(400);
    const sortField = ref('');
    const sortOrder = ref('');
    const filterModalVisible = ref(false);
    const currentFilterColumn = ref(null);
    const filterCondition = ref('contains');
    const filterValue = ref('');
    const filters = ref({});
    
    // 计算属性
    const filteredData = computed(() => {
      let result = [...props.data];
      
      // 应用搜索
      if (searchText.value) {
        const searchLower = searchText.value.toLowerCase();
        result = result.filter(item => {
          return Object.values(item).some(val => {
            if (val === null || val === undefined) return false;
            return String(val).toLowerCase().includes(searchLower);
          });
        });
      }
      
      // 应用筛选
      Object.keys(filters.value).forEach(key => {
        const filter = filters.value[key];
        if (filter.value) {
          result = result.filter(item => {
            const val = item[key];
            if (val === null || val === undefined) return false;
            
            const strVal = String(val).toLowerCase();
            const filterVal = filter.value.toLowerCase();
            
            switch (filter.condition) {
              case 'equals':
                return strVal === filterVal;
              case 'notEquals':
                return strVal !== filterVal;
              case 'contains':
                return strVal.includes(filterVal);
              case 'notContains':
                return !strVal.includes(filterVal);
              case 'startsWith':
                return strVal.startsWith(filterVal);
              case 'endsWith':
                return strVal.endsWith(filterVal);
              case 'greaterThan':
                return Number(val) > Number(filterVal);
              case 'lessThan':
                return Number(val) < Number(filterVal);
              default:
                return true;
            }
          });
        }
      });
      
      // 应用排序
      if (sortField.value && sortOrder.value) {
        result.sort((a, b) => {
          const aVal = a[sortField.value];
          const bVal = b[sortField.value];
          
          // 处理空值
          if (aVal === undefined || aVal === null) return sortOrder.value === 'asc' ? -1 : 1;
          if (bVal === undefined || bVal === null) return sortOrder.value === 'asc' ? 1 : -1;
          
          // 数字比较
          if (typeof aVal === 'number' && typeof bVal === 'number') {
            return sortOrder.value === 'asc' ? aVal - bVal : bVal - aVal;
          }
          
          // 字符串比较
          const aStr = String(aVal);
          const bStr = String(bVal);
          return sortOrder.value === 'asc' 
            ? aStr.localeCompare(bStr) 
            : bStr.localeCompare(aStr);
        });
      }
      
      return result;
    });
    
    // 总记录数
    const totalRecords = computed(() => {
      return filteredData.value.length;
    });
    
    // 分页配置
    const pagination = computed(() => {
      return {
        current: currentPage.value,
        pageSize: pageSize.value,
        total: totalRecords.value,
        showSizeChanger: false,
        showQuickJumper: true,
        showTotal: (total) => `共 ${total} 条`
      };
    });
    
    // 监听窗口大小变化，调整表格高度
    const updateTableHeight = () => {
      const containerHeight = document.querySelector('.data-table-container')?.clientHeight;
      if (!containerHeight) return;
      
      let height = containerHeight;
      
      // 减去工具栏高度
      if (props.showToolbar) {
        height -= 56; // 工具栏高度
      }
      
      // 减去分页器高度
      height -= 56; // 分页器高度
      
      // 减去表格头部高度
      height -= 55; // 表头高度
      
      // 减去表格底部高度
      if (props.showFooter) {
        height -= 40; // 底部高度
      }
      
      // 设置最小高度
      height = Math.max(height, 200);
      
      tableHeight.value = height;
    };
    
    // 监听数据变化，更新表格高度
    watch(() => props.data, () => {
      updateTableHeight();
    });
    
    // 组件挂载时，更新表格高度
    onMounted(() => {
      updateTableHeight();
      window.addEventListener('resize', updateTableHeight);
    });
    
    // 组件卸载时，移除事件监听
    onMounted(() => {
      return () => {
        window.removeEventListener('resize', updateTableHeight);
      };
    });
    
    // 搜索
    const onSearch = () => {
      currentPage.value = 1;
      emit('search', searchText.value);
    };
    
    // 更改每页显示数量
    const onPageSizeChange = (size) => {
      pageSize.value = size;
      currentPage.value = 1;
    };
    
    // 表格变化事件
    const handleTableChange = (pagination) => {
      currentPage.value = pagination.current;
    };
    
    // 排序列
    const sortColumn = (field, order) => {
      sortField.value = field;
      sortOrder.value = order;
    };
    
    // 显示筛选弹窗
    const showFilterModal = (column) => {
      currentFilterColumn.value = column;
      
      // 如果已有筛选，则加载已有的筛选条件
      if (filters.value[column.dataIndex]) {
        filterCondition.value = filters.value[column.dataIndex].condition;
        filterValue.value = filters.value[column.dataIndex].value;
      } else {
        filterCondition.value = 'contains';
        filterValue.value = '';
      }
      
      filterModalVisible.value = true;
    };
    
    // 应用筛选
    const applyFilter = () => {
      if (!currentFilterColumn.value) return;
      
      const field = currentFilterColumn.value.dataIndex;
      
      if (filterValue.value) {
        filters.value[field] = {
          condition: filterCondition.value,
          value: filterValue.value
        };
      } else {
        // 如果筛选值为空，则移除筛选
        delete filters.value[field];
      }
      
      filterModalVisible.value = false;
      currentPage.value = 1;
    };
    
    // 取消筛选
    const cancelFilter = () => {
      filterModalVisible.value = false;
    };
    
    // 刷新数据
    const refreshData = () => {
      emit('refresh');
    };
    
    // 导出数据
    const exportData = (type) => {
      emit('export', {
        type,
        data: filteredData.value,
        columns: props.columns
      });
    };
    
    // 复制数据到剪贴板
    const copyToClipboard = () => {
      try {
        // 创建表格数据的文本表示
        const headers = props.columns.map(col => col.title).join('\t');
        const rows = filteredData.value.map(row => {
          return props.columns
            .map(col => row[col.dataIndex] !== undefined ? row[col.dataIndex] : '')
            .join('\t');
        }).join('\n');
        
        const text = `${headers}\n${rows}`;
        
        navigator.clipboard.writeText(text).then(() => {
          message.success('数据已复制到剪贴板');
        });
      } catch (error) {
        console.error('复制失败:', error);
        message.error('复制失败');
      }
    };
    
    return {
      searchText,
      pageSize,
      currentPage,
      tableHeight,
      filteredData,
      totalRecords,
      pagination,
      filterModalVisible,
      currentFilterColumn,
      filterCondition,
      filterValue,
      onSearch,
      onPageSizeChange,
      handleTableChange,
      sortColumn,
      showFilterModal,
      applyFilter,
      cancelFilter,
      refreshData,
      exportData,
      copyToClipboard
    };
  }
});
</script>

<style scoped>
.data-table-container {
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

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--component-background);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-wrapper {
  flex: 1;
  overflow: hidden;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-top: 1px solid var(--border-color);
  background-color: var(--component-background);
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
}

:deep(.ant-table-thead > tr > th) {
  position: relative;
  padding-right: 28px !important;
}

.column-menu-trigger {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

:deep(.ant-table-cell) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 深色主题适配 */
:global(.dark-theme) .data-table-container {
  border-color: #303030;
}

:global(.dark-theme) .table-toolbar,
:global(.dark-theme) .table-footer {
  background-color: #1f1f1f;
  border-color: #303030;
}

:global(.dark-theme) .column-menu-trigger {
  color: rgba(255, 255, 255, 0.45);
}
</style> 