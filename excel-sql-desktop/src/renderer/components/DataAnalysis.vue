<template>
  <div class="data-analysis-container">
    <a-spin :spinning="loading" tip="分析中...">
      <a-card class="analysis-card" :bordered="false">
        <template #title>
          <div class="card-title">
            <bar-chart-outlined /> 数据分析
            <a-tooltip title="刷新分析">
              <reload-outlined class="refresh-icon" @click="refreshAnalysis" />
            </a-tooltip>
          </div>
        </template>
        
        <div class="analysis-content">
          <div v-if="!hasData" class="empty-data">
            <inbox-outlined class="empty-icon" />
            <p>没有可用数据进行分析</p>
            <a-button type="primary" @click="$emit('select-data')">选择数据</a-button>
          </div>
          
          <template v-else>
            <!-- 数据概览 -->
            <div class="analysis-section">
              <h3 class="section-title">数据概览</h3>
              <a-row :gutter="[16, 16]">
                <a-col :span="8">
                  <a-statistic 
                    title="总行数" 
                    :value="statistics.rowCount" 
                    :valueStyle="{ color: '#1890ff' }" 
                  >
                    <template #prefix>
                      <ordered-list-outlined />
                    </template>
                  </a-statistic>
                </a-col>
                <a-col :span="8">
                  <a-statistic 
                    title="总列数" 
                    :value="statistics.columnCount" 
                    :valueStyle="{ color: '#52c41a' }" 
                  >
                    <template #prefix>
                      <column-width-outlined />
                    </template>
                  </a-statistic>
                </a-col>
                <a-col :span="8">
                  <a-statistic 
                    title="数据单元格" 
                    :value="statistics.cellCount" 
                    :valueStyle="{ color: '#722ed1' }" 
                  >
                    <template #prefix>
                      <table-outlined />
                    </template>
                  </a-statistic>
                </a-col>
              </a-row>
            </div>
            
            <!-- 列统计信息 -->
            <div class="analysis-section">
              <h3 class="section-title">列统计信息</h3>
              <a-tabs>
                <a-tab-pane key="numeric" tab="数值列">
                  <div v-if="numericColumns.length === 0" class="empty-type">
                    <p>没有数值类型的列</p>
                  </div>
                  <a-table 
                    v-else
                    :columns="numericStatsColumns" 
                    :data-source="numericStats" 
                    :pagination="false"
                    size="small"
                    :scroll="{ x: 800 }"
                  />
                </a-tab-pane>
                <a-tab-pane key="text" tab="文本列">
                  <div v-if="textColumns.length === 0" class="empty-type">
                    <p>没有文本类型的列</p>
                  </div>
                  <a-table 
                    v-else
                    :columns="textStatsColumns" 
                    :data-source="textStats" 
                    :pagination="false"
                    size="small"
                    :scroll="{ x: 800 }"
                  />
                </a-tab-pane>
                <a-tab-pane key="date" tab="日期列">
                  <div v-if="dateColumns.length === 0" class="empty-type">
                    <p>没有日期类型的列</p>
                  </div>
                  <a-table 
                    v-else
                    :columns="dateStatsColumns" 
                    :data-source="dateStats" 
                    :pagination="false"
                    size="small"
                    :scroll="{ x: 800 }"
                  />
                </a-tab-pane>
              </a-tabs>
            </div>
            
            <!-- 数据质量 -->
            <div class="analysis-section">
              <h3 class="section-title">数据质量</h3>
              <a-row :gutter="[16, 16]">
                <a-col :span="12">
                  <a-card title="空值分析" size="small">
                    <a-progress
                      :percent="statistics.nullPercentage"
                      :stroke-color="statistics.nullPercentage > 20 ? '#ff4d4f' : '#1890ff'"
                      size="small"
                    />
                    <div class="quality-stats">
                      <p>空值单元格: {{ statistics.nullCount }}</p>
                      <p>空值比例: {{ statistics.nullPercentage.toFixed(2) }}%</p>
                    </div>
                  </a-card>
                </a-col>
                <a-col :span="12">
                  <a-card title="重复值分析" size="small">
                    <a-progress
                      :percent="statistics.duplicatePercentage"
                      :stroke-color="statistics.duplicatePercentage > 20 ? '#faad14' : '#52c41a'"
                      size="small"
                    />
                    <div class="quality-stats">
                      <p>重复行数: {{ statistics.duplicateCount }}</p>
                      <p>重复比例: {{ statistics.duplicatePercentage.toFixed(2) }}%</p>
                    </div>
                  </a-card>
                </a-col>
              </a-row>
            </div>
            
            <!-- 操作建议 -->
            <div class="analysis-section">
              <h3 class="section-title">操作建议</h3>
              <a-alert 
                v-for="(suggestion, index) in suggestions" 
                :key="index"
                :message="suggestion.title" 
                :description="suggestion.description"
                :type="suggestion.type"
                class="suggestion-alert"
                show-icon
              />
            </div>
          </template>
        </div>
      </a-card>
    </a-spin>
  </div>
</template>

<script>
import { defineComponent, ref, computed, watch } from 'vue';
import { 
  BarChartOutlined, 
  ReloadOutlined, 
  InboxOutlined, 
  OrderedListOutlined,
  ColumnWidthOutlined,
  TableOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'DataAnalysis',
  components: {
    BarChartOutlined,
    ReloadOutlined,
    InboxOutlined,
    OrderedListOutlined,
    ColumnWidthOutlined,
    TableOutlined
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
    }
  },
  emits: ['refresh', 'select-data'],
  setup(props, { emit }) {
    // 判断是否有数据
    const hasData = computed(() => {
      return props.data && props.data.length > 0 && props.columns && props.columns.length > 0;
    });
    
    // 基础统计信息
    const statistics = computed(() => {
      if (!hasData.value) {
        return {
          rowCount: 0,
          columnCount: 0,
          cellCount: 0,
          nullCount: 0,
          nullPercentage: 0,
          duplicateCount: 0,
          duplicatePercentage: 0
        };
      }
      
      const rowCount = props.data.length;
      const columnCount = props.columns.length;
      const cellCount = rowCount * columnCount;
      
      // 计算空值数量
      let nullCount = 0;
      props.data.forEach(row => {
        Object.values(row).forEach(value => {
          if (value === null || value === undefined || value === '') {
            nullCount++;
          }
        });
      });
      
      // 计算重复行数量
      const uniqueRows = new Set();
      let duplicateCount = 0;
      
      props.data.forEach(row => {
        const rowKey = JSON.stringify(row);
        if (uniqueRows.has(rowKey)) {
          duplicateCount++;
        } else {
          uniqueRows.add(rowKey);
        }
      });
      
      return {
        rowCount,
        columnCount,
        cellCount,
        nullCount,
        nullPercentage: (nullCount / cellCount) * 100,
        duplicateCount,
        duplicatePercentage: (duplicateCount / rowCount) * 100
      };
    });
    
    // 数值列
    const numericColumns = computed(() => {
      if (!hasData.value) return [];
      
      return props.columns.filter(col => {
        // 检查列中的值是否主要是数字
        const sampleSize = Math.min(props.data.length, 10);
        let numericCount = 0;
        
        for (let i = 0; i < sampleSize; i++) {
          const value = props.data[i][col.dataIndex];
          if (typeof value === 'number' || (typeof value === 'string' && !isNaN(parseFloat(value)))) {
            numericCount++;
          }
        }
        
        return numericCount / sampleSize >= 0.7; // 如果70%以上的样本是数字，则认为是数值列
      });
    });
    
    // 文本列
    const textColumns = computed(() => {
      if (!hasData.value) return [];
      
      return props.columns.filter(col => {
        // 检查列中的值是否主要是文本
        const sampleSize = Math.min(props.data.length, 10);
        let textCount = 0;
        
        for (let i = 0; i < sampleSize; i++) {
          const value = props.data[i][col.dataIndex];
          if (typeof value === 'string' && isNaN(parseFloat(value))) {
            textCount++;
          }
        }
        
        return textCount / sampleSize >= 0.7; // 如果70%以上的样本是文本，则认为是文本列
      });
    });
    
    // 日期列
    const dateColumns = computed(() => {
      if (!hasData.value) return [];
      
      return props.columns.filter(col => {
        // 检查列中的值是否主要是日期
        const sampleSize = Math.min(props.data.length, 10);
        let dateCount = 0;
        
        for (let i = 0; i < sampleSize; i++) {
          const value = props.data[i][col.dataIndex];
          if (value instanceof Date || (typeof value === 'string' && !isNaN(Date.parse(value)))) {
            dateCount++;
          }
        }
        
        return dateCount / sampleSize >= 0.7; // 如果70%以上的样本是日期，则认为是日期列
      });
    });
    
    // 数值统计列定义
    const numericStatsColumns = [
      { title: '列名', dataIndex: 'column', key: 'column' },
      { title: '最小值', dataIndex: 'min', key: 'min' },
      { title: '最大值', dataIndex: 'max', key: 'max' },
      { title: '平均值', dataIndex: 'avg', key: 'avg' },
      { title: '中位数', dataIndex: 'median', key: 'median' },
      { title: '标准差', dataIndex: 'stdDev', key: 'stdDev' },
      { title: '空值数', dataIndex: 'nullCount', key: 'nullCount' },
      { title: '空值率', dataIndex: 'nullRate', key: 'nullRate' }
    ];
    
    // 文本统计列定义
    const textStatsColumns = [
      { title: '列名', dataIndex: 'column', key: 'column' },
      { title: '唯一值数', dataIndex: 'uniqueCount', key: 'uniqueCount' },
      { title: '最长值', dataIndex: 'maxLength', key: 'maxLength' },
      { title: '最短值', dataIndex: 'minLength', key: 'minLength' },
      { title: '平均长度', dataIndex: 'avgLength', key: 'avgLength' },
      { title: '空值数', dataIndex: 'nullCount', key: 'nullCount' },
      { title: '空值率', dataIndex: 'nullRate', key: 'nullRate' }
    ];
    
    // 日期统计列定义
    const dateStatsColumns = [
      { title: '列名', dataIndex: 'column', key: 'column' },
      { title: '最早日期', dataIndex: 'minDate', key: 'minDate' },
      { title: '最晚日期', dataIndex: 'maxDate', key: 'maxDate' },
      { title: '日期范围(天)', dataIndex: 'range', key: 'range' },
      { title: '空值数', dataIndex: 'nullCount', key: 'nullCount' },
      { title: '空值率', dataIndex: 'nullRate', key: 'nullRate' }
    ];
    
    // 数值统计数据
    const numericStats = computed(() => {
      if (!hasData.value || numericColumns.value.length === 0) return [];
      
      return numericColumns.value.map(col => {
        const values = props.data
          .map(row => {
            const val = row[col.dataIndex];
            return typeof val === 'string' ? parseFloat(val) : val;
          })
          .filter(val => val !== null && val !== undefined && !isNaN(val));
        
        // 计算统计值
        const sortedValues = [...values].sort((a, b) => a - b);
        const min = sortedValues[0] || 0;
        const max = sortedValues[sortedValues.length - 1] || 0;
        const sum = sortedValues.reduce((acc, val) => acc + val, 0);
        const avg = sortedValues.length > 0 ? sum / sortedValues.length : 0;
        
        // 中位数
        const midIndex = Math.floor(sortedValues.length / 2);
        const median = sortedValues.length % 2 === 0
          ? (sortedValues[midIndex - 1] + sortedValues[midIndex]) / 2
          : sortedValues[midIndex];
        
        // 标准差
        const variance = sortedValues.reduce((acc, val) => acc + Math.pow(val - avg, 2), 0) / sortedValues.length;
        const stdDev = Math.sqrt(variance);
        
        // 空值统计
        const nullCount = props.data.filter(row => {
          const val = row[col.dataIndex];
          return val === null || val === undefined || val === '' || isNaN(val);
        }).length;
        
        return {
          key: col.dataIndex,
          column: col.title,
          min: min.toFixed(2),
          max: max.toFixed(2),
          avg: avg.toFixed(2),
          median: median.toFixed(2),
          stdDev: stdDev.toFixed(2),
          nullCount,
          nullRate: ((nullCount / props.data.length) * 100).toFixed(2) + '%'
        };
      });
    });
    
    // 文本统计数据
    const textStats = computed(() => {
      if (!hasData.value || textColumns.value.length === 0) return [];
      
      return textColumns.value.map(col => {
        const values = props.data
          .map(row => row[col.dataIndex])
          .filter(val => val !== null && val !== undefined);
        
        // 唯一值数量
        const uniqueValues = new Set(values);
        
        // 长度统计
        const lengths = values.map(val => String(val).length);
        const sortedLengths = [...lengths].sort((a, b) => a - b);
        
        const minLength = sortedLengths[0] || 0;
        const maxLength = sortedLengths[sortedLengths.length - 1] || 0;
        const sumLength = sortedLengths.reduce((acc, val) => acc + val, 0);
        const avgLength = sortedLengths.length > 0 ? sumLength / sortedLengths.length : 0;
        
        // 空值统计
        const nullCount = props.data.filter(row => {
          const val = row[col.dataIndex];
          return val === null || val === undefined || val === '';
        }).length;
        
        return {
          key: col.dataIndex,
          column: col.title,
          uniqueCount: uniqueValues.size,
          minLength,
          maxLength,
          avgLength: avgLength.toFixed(2),
          nullCount,
          nullRate: ((nullCount / props.data.length) * 100).toFixed(2) + '%'
        };
      });
    });
    
    // 日期统计数据
    const dateStats = computed(() => {
      if (!hasData.value || dateColumns.value.length === 0) return [];
      
      return dateColumns.value.map(col => {
        const values = props.data
          .map(row => {
            const val = row[col.dataIndex];
            if (val instanceof Date) return val;
            if (typeof val === 'string') {
              const date = new Date(val);
              return isNaN(date) ? null : date;
            }
            return null;
          })
          .filter(val => val !== null);
        
        // 日期范围
        const sortedDates = [...values].sort((a, b) => a - b);
        const minDate = sortedDates[0];
        const maxDate = sortedDates[sortedDates.length - 1];
        
        // 计算日期范围（天数）
        const range = minDate && maxDate 
          ? Math.round((maxDate - minDate) / (1000 * 60 * 60 * 24)) 
          : 0;
        
        // 空值统计
        const nullCount = props.data.filter(row => {
          const val = row[col.dataIndex];
          return val === null || val === undefined || val === '' || (typeof val === 'string' && isNaN(Date.parse(val)));
        }).length;
        
        return {
          key: col.dataIndex,
          column: col.title,
          minDate: minDate ? minDate.toLocaleDateString() : '-',
          maxDate: maxDate ? maxDate.toLocaleDateString() : '-',
          range,
          nullCount,
          nullRate: ((nullCount / props.data.length) * 100).toFixed(2) + '%'
        };
      });
    });
    
    // 数据质量建议
    const suggestions = computed(() => {
      if (!hasData.value) return [];
      
      const result = [];
      
      // 空值建议
      if (statistics.value.nullPercentage > 10) {
        result.push({
          title: '数据存在较多空值',
          description: `数据中有 ${statistics.value.nullPercentage.toFixed(2)}% 的单元格为空值，建议检查数据完整性或使用填充方法处理空值。`,
          type: 'warning'
        });
      }
      
      // 重复值建议
      if (statistics.value.duplicatePercentage > 5) {
        result.push({
          title: '数据存在重复行',
          description: `数据中有 ${statistics.value.duplicatePercentage.toFixed(2)}% 的行是重复的，建议检查数据唯一性或使用去重功能。`,
          type: 'warning'
        });
      }
      
      // 数值列异常值检测
      numericStats.value.forEach(stat => {
        const min = parseFloat(stat.min);
        const max = parseFloat(stat.max);
        const avg = parseFloat(stat.avg);
        const stdDev = parseFloat(stat.stdDev);
        
        // 检查是否存在异常值（超过平均值正负3个标准差）
        if (max > avg + 3 * stdDev || min < avg - 3 * stdDev) {
          result.push({
            title: `列 "${stat.column}" 可能存在异常值`,
            description: `该列数据范围为 ${min} 到 ${max}，平均值为 ${avg}，存在可能的异常值。建议使用箱线图或散点图进行可视化分析。`,
            type: 'info'
          });
        }
      });
      
      // 如果没有问题，添加一个正面反馈
      if (result.length === 0) {
        result.push({
          title: '数据质量良好',
          description: '未检测到明显的数据质量问题。',
          type: 'success'
        });
      }
      
      return result;
    });
    
    // 刷新分析
    const refreshAnalysis = () => {
      emit('refresh');
    };
    
    return {
      hasData,
      statistics,
      numericColumns,
      textColumns,
      dateColumns,
      numericStatsColumns,
      textStatsColumns,
      dateStatsColumns,
      numericStats,
      textStats,
      dateStats,
      suggestions,
      refreshAnalysis
    };
  }
});
</script>

<style scoped>
.data-analysis-container {
  height: 100%;
  overflow: auto;
  padding: 16px;
}

.analysis-card {
  height: 100%;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.refresh-icon {
  cursor: pointer;
  font-size: 16px;
  color: var(--text-color-secondary);
  transition: color 0.3s;
}

.refresh-icon:hover {
  color: var(--primary-color);
}

.analysis-content {
  height: 100%;
  overflow: auto;
}

/* 确保表格内容可以滚动 */
.analysis-section :deep(.ant-table-body) {
  overflow-y: auto !important;
  max-height: 300px !important;
}

.empty-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
}

.empty-icon {
  font-size: 48px;
  color: var(--disabled-color);
  margin-bottom: 16px;
}

.analysis-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  position: relative;
  padding-left: 12px;
  color: var(--text-color);
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: var(--primary-color);
  border-radius: 2px;
}

.empty-type {
  text-align: center;
  padding: 16px;
  color: var(--text-color-secondary);
}

.quality-stats {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
}

.quality-stats p {
  margin: 0;
  color: var(--text-color-secondary);
}

.suggestion-alert {
  margin-bottom: 8px;
}
</style> 