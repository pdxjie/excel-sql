<template>
  <div class="chart-container">
    <a-spin :spinning="loading" tip="加载中...">
      <div class="chart-toolbar">
        <div class="toolbar-left">
          <a-select 
            v-model:value="chartType" 
            style="width: 120px" 
            @change="handleChartTypeChange"
          >
            <a-select-option value="bar">柱状图</a-select-option>
            <a-select-option value="line">折线图</a-select-option>
            <a-select-option value="pie">饼图</a-select-option>
            <a-select-option value="scatter">散点图</a-select-option>
          </a-select>
          
          <a-select 
            v-if="['bar', 'line', 'scatter'].includes(chartType)"
            v-model:value="xField" 
            style="width: 150px; margin-left: 8px;" 
            placeholder="选择X轴字段"
            @change="updateChart"
          >
            <a-select-option v-for="col in columns" :key="col.dataIndex" :value="col.dataIndex">
              {{ col.title }}
            </a-select-option>
          </a-select>
          
          <a-select 
            v-if="['bar', 'line', 'scatter'].includes(chartType)"
            v-model:value="yField" 
            style="width: 150px; margin-left: 8px;" 
            placeholder="选择Y轴字段"
            @change="updateChart"
          >
            <a-select-option v-for="col in numericColumns" :key="col.dataIndex" :value="col.dataIndex">
              {{ col.title }}
            </a-select-option>
          </a-select>
          
          <a-select 
            v-if="chartType === 'pie'"
            v-model:value="categoryField" 
            style="width: 150px; margin-left: 8px;" 
            placeholder="选择分类字段"
            @change="updateChart"
          >
            <a-select-option v-for="col in columns" :key="col.dataIndex" :value="col.dataIndex">
              {{ col.title }}
            </a-select-option>
          </a-select>
          
          <a-select 
            v-if="chartType === 'pie'"
            v-model:value="valueField" 
            style="width: 150px; margin-left: 8px;" 
            placeholder="选择数值字段"
            @change="updateChart"
          >
            <a-select-option v-for="col in numericColumns" :key="col.dataIndex" :value="col.dataIndex">
              {{ col.title }}
            </a-select-option>
          </a-select>
        </div>
        
        <div class="toolbar-right">
          <a-button-group>
            <a-tooltip title="刷新图表">
              <a-button type="text" @click="refreshChart">
                <template #icon><reload-outlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="导出图表">
              <a-button type="text" @click="exportChart">
                <template #icon><download-outlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="全屏查看">
              <a-button type="text" @click="toggleFullscreen">
                <template #icon><fullscreen-outlined /></template>
              </a-button>
            </a-tooltip>
          </a-button-group>
        </div>
      </div>
      
      <div class="chart-content">
        <div v-if="!hasData" class="empty-data">
          <area-chart-outlined class="empty-icon" />
          <p>没有可用数据进行可视化</p>
          <a-button type="primary" @click="$emit('select-data')">选择数据</a-button>
        </div>
        
        <div v-else-if="!isChartReady" class="chart-config">
          <p>请选择图表类型和数据字段</p>
        </div>
        
        <div v-else ref="chartRef" class="chart-canvas"></div>
      </div>
      
      <a-modal
        v-model:visible="fullscreenVisible"
        title="图表全屏查看"
        :footer="null"
        width="90%"
        style="top: 20px"
        :destroyOnClose="true"
      >
        <div ref="fullscreenChartRef" class="fullscreen-chart"></div>
      </a-modal>
    </a-spin>
  </div>
</template>

<script>
import { defineComponent, ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import * as echarts from 'echarts/core';
import { 
  BarChart, 
  LineChart, 
  PieChart, 
  ScatterChart 
} from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  ToolboxComponent
} from 'echarts/components';
import { LabelLayout, UniversalTransition } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';
import { 
  ReloadOutlined, 
  DownloadOutlined, 
  FullscreenOutlined,
  AreaChartOutlined
} from '@ant-design/icons-vue';

// 注册必须的组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  ToolboxComponent,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer,
  BarChart,
  LineChart,
  PieChart,
  ScatterChart
]);

export default defineComponent({
  name: 'DataChart',
  components: {
    ReloadOutlined,
    DownloadOutlined,
    FullscreenOutlined,
    AreaChartOutlined
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
    // 图表实例
    const chartRef = ref(null);
    const fullscreenChartRef = ref(null);
    let chartInstance = null;
    let fullscreenChartInstance = null;
    
    // 图表配置
    const chartType = ref('bar');
    const xField = ref('');
    const yField = ref('');
    const categoryField = ref('');
    const valueField = ref('');
    const fullscreenVisible = ref(false);
    
    // 判断是否有数据
    const hasData = computed(() => {
      return props.data && props.data.length > 0 && props.columns && props.columns.length > 0;
    });
    
    // 判断图表是否准备好
    const isChartReady = computed(() => {
      if (chartType.value === 'bar' || chartType.value === 'line' || chartType.value === 'scatter') {
        return xField.value && yField.value;
      } else if (chartType.value === 'pie') {
        return categoryField.value && valueField.value;
      }
      return false;
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
    
    // 初始化图表
    const initChart = () => {
      if (chartInstance) {
        chartInstance.dispose();
      }
      
      if (chartRef.value) {
        chartInstance = echarts.init(chartRef.value);
        updateChart();
      }
    };
    
    // 初始化全屏图表
    const initFullscreenChart = () => {
      if (fullscreenChartInstance) {
        fullscreenChartInstance.dispose();
      }
      
      if (fullscreenChartRef.value) {
        fullscreenChartInstance = echarts.init(fullscreenChartRef.value);
        updateFullscreenChart();
      }
    };
    
    // 更新图表
    const updateChart = () => {
      if (!chartInstance || !isChartReady.value) return;
      
      const option = getChartOption();
      chartInstance.setOption(option);
    };
    
    // 更新全屏图表
    const updateFullscreenChart = () => {
      if (!fullscreenChartInstance || !isChartReady.value) return;
      
      const option = getChartOption();
      fullscreenChartInstance.setOption(option);
    };
    
    // 获取图表配置
    const getChartOption = () => {
      const baseOption = {
        title: {
          text: getChartTitle(),
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'horizontal',
          bottom: 10
        },
        toolbox: {
          feature: {
            saveAsImage: { title: '保存为图片' },
            dataView: { title: '数据视图', lang: ['数据视图', '关闭', '刷新'] }
          }
        }
      };
      
      if (chartType.value === 'bar') {
        return {
          ...baseOption,
          xAxis: {
            type: 'category',
            data: props.data.map(item => item[xField.value]),
            name: getFieldName(xField.value)
          },
          yAxis: {
            type: 'value',
            name: getFieldName(yField.value)
          },
          series: [
            {
              data: props.data.map(item => {
                const value = item[yField.value];
                return typeof value === 'string' ? parseFloat(value) : value;
              }),
              type: 'bar'
            }
          ]
        };
      } else if (chartType.value === 'line') {
        return {
          ...baseOption,
          xAxis: {
            type: 'category',
            data: props.data.map(item => item[xField.value]),
            name: getFieldName(xField.value)
          },
          yAxis: {
            type: 'value',
            name: getFieldName(yField.value)
          },
          series: [
            {
              data: props.data.map(item => {
                const value = item[yField.value];
                return typeof value === 'string' ? parseFloat(value) : value;
              }),
              type: 'line',
              smooth: true
            }
          ]
        };
      } else if (chartType.value === 'pie') {
        // 对数据进行分组和聚合
        const groupedData = {};
        props.data.forEach(item => {
          const category = item[categoryField.value];
          const value = item[valueField.value];
          const numValue = typeof value === 'string' ? parseFloat(value) : value;
          
          if (category && !isNaN(numValue)) {
            if (!groupedData[category]) {
              groupedData[category] = 0;
            }
            groupedData[category] += numValue;
          }
        });
        
        const pieData = Object.entries(groupedData).map(([name, value]) => ({ name, value }));
        
        return {
          ...baseOption,
          series: [
            {
              name: getFieldName(valueField.value),
              type: 'pie',
              radius: '50%',
              data: pieData,
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
              }
            }
          ]
        };
      } else if (chartType.value === 'scatter') {
        return {
          ...baseOption,
          xAxis: {
            type: 'value',
            name: getFieldName(xField.value)
          },
          yAxis: {
            type: 'value',
            name: getFieldName(yField.value)
          },
          series: [
            {
              data: props.data.map(item => {
                const xValue = item[xField.value];
                const yValue = item[yField.value];
                const x = typeof xValue === 'string' ? parseFloat(xValue) : xValue;
                const y = typeof yValue === 'string' ? parseFloat(yValue) : yValue;
                return [x, y];
              }),
              type: 'scatter'
            }
          ]
        };
      }
      
      return baseOption;
    };
    
    // 获取字段名称
    const getFieldName = (fieldKey) => {
      const column = props.columns.find(col => col.dataIndex === fieldKey);
      return column ? column.title : fieldKey;
    };
    
    // 获取图表标题
    const getChartTitle = () => {
      if (chartType.value === 'bar' || chartType.value === 'line' || chartType.value === 'scatter') {
        return `${getFieldName(yField.value)} vs ${getFieldName(xField.value)}`;
      } else if (chartType.value === 'pie') {
        return `${getFieldName(categoryField.value)} 的 ${getFieldName(valueField.value)} 分布`;
      }
      return '数据可视化';
    };
    
    // 处理图表类型变更
    const handleChartTypeChange = () => {
      updateChart();
    };
    
    // 刷新图表
    const refreshChart = () => {
      emit('refresh');
      nextTick(() => {
        updateChart();
      });
    };
    
    // 导出图表
    const exportChart = () => {
      if (!chartInstance) return;
      
      try {
        const dataURL = chartInstance.getDataURL({
          pixelRatio: 2,
          backgroundColor: '#fff'
        });
        
        // 创建下载链接
        const link = document.createElement('a');
        link.download = `chart-${new Date().getTime()}.png`;
        link.href = dataURL;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        message.success('图表已导出');
      } catch (error) {
        console.error('导出图表失败:', error);
        message.error('导出图表失败');
      }
    };
    
    // 切换全屏
    const toggleFullscreen = () => {
      fullscreenVisible.value = true;
      nextTick(() => {
        initFullscreenChart();
      });
    };
    
    // 监听窗口大小变化
    const handleResize = () => {
      if (chartInstance) {
        chartInstance.resize();
      }
      if (fullscreenChartInstance && fullscreenVisible.value) {
        fullscreenChartInstance.resize();
      }
    };
    
    // 监听数据变化
    watch(() => props.data, () => {
      nextTick(() => {
        updateChart();
        if (fullscreenVisible.value) {
          updateFullscreenChart();
        }
      });
    }, { deep: true });
    
    // 监听列变化
    watch(() => props.columns, () => {
      nextTick(() => {
        // 如果当前选择的字段不在新的列中，则重置
        const columnKeys = props.columns.map(col => col.dataIndex);
        if (xField.value && !columnKeys.includes(xField.value)) {
          xField.value = '';
        }
        if (yField.value && !columnKeys.includes(yField.value)) {
          yField.value = '';
        }
        if (categoryField.value && !columnKeys.includes(categoryField.value)) {
          categoryField.value = '';
        }
        if (valueField.value && !columnKeys.includes(valueField.value)) {
          valueField.value = '';
        }
        
        updateChart();
        if (fullscreenVisible.value) {
          updateFullscreenChart();
        }
      });
    }, { deep: true });
    
    // 监听全屏状态
    watch(() => fullscreenVisible.value, (val) => {
      if (!val && fullscreenChartInstance) {
        fullscreenChartInstance.dispose();
        fullscreenChartInstance = null;
      }
    });
    
    // 组件挂载时
    onMounted(() => {
      nextTick(() => {
        initChart();
        window.addEventListener('resize', handleResize);
      });
    });
    
    // 组件卸载前
    onBeforeUnmount(() => {
      window.removeEventListener('resize', handleResize);
      if (chartInstance) {
        chartInstance.dispose();
      }
      if (fullscreenChartInstance) {
        fullscreenChartInstance.dispose();
      }
    });
    
    return {
      chartRef,
      fullscreenChartRef,
      chartType,
      xField,
      yField,
      categoryField,
      valueField,
      fullscreenVisible,
      hasData,
      isChartReady,
      numericColumns,
      handleChartTypeChange,
      refreshChart,
      exportChart,
      toggleFullscreen,
      updateChart
    };
  }
});
</script>

<style scoped>
.chart-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--component-background);
  border-radius: var(--border-radius-base);
}

.chart-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.chart-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.empty-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.empty-icon {
  font-size: 48px;
  color: var(--disabled-color);
  margin-bottom: 16px;
}

.chart-config {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-color-secondary);
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

.fullscreen-chart {
  width: 100%;
  height: 70vh;
}

/* 修复移动设备上的工具栏显示 */
@media (max-width: 768px) {
  .chart-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .toolbar-left, .toolbar-right {
    width: 100%;
  }
  
  .toolbar-right {
    margin-top: 8px;
    justify-content: flex-end;
  }
}
</style> 