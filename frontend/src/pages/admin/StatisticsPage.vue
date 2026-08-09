<template>
  <div class="statistics-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-container">
        <div class="header-content">
          <h1 class="page-title">数据分析</h1>
          <p class="page-subtitle">系统运营数据概览</p>
        </div>
        <a-button @click="loadData" :loading="loading" class="refresh-btn">
          <template #icon>
            <ReloadOutlined />
          </template>
          刷新数据
        </a-button>
      </div>
    </div>

    <div class="container">
      <a-spin :spinning="loading" tip="加载中...">
        <!-- 核心指标卡片 -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(34, 197, 94, 0.1)">
              <FileTextOutlined style="color: var(--color-primary)" />
            </div>
            <div class="stat-content">
              <div class="stat-label">今日创作</div>
              <div class="stat-value">{{ stats?.todayCount ?? 0 }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(59, 130, 246, 0.1)">
              <BarChartOutlined style="color: #3B82F6" />
            </div>
            <div class="stat-content">
              <div class="stat-label">本周创作</div>
              <div class="stat-value">{{ stats?.weekCount ?? 0 }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(168, 85, 247, 0.1)">
              <RiseOutlined style="color: #A855F7" />
            </div>
            <div class="stat-content">
              <div class="stat-label">本月创作</div>
              <div class="stat-value">{{ stats?.monthCount ?? 0 }}</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(234, 179, 8, 0.1)">
              <CheckCircleOutlined style="color: #EAB308" />
            </div>
            <div class="stat-content">
              <div class="stat-label">成功率</div>
              <div class="stat-value">{{ (stats?.successRate ?? 0).toFixed(1) }}%</div>
            </div>
          </div>
        </div>

        <!-- 图表区域 -->
        <div class="charts-grid">
          <!-- 创作趋势图 -->
          <a-card :bordered="false" class="chart-card">
            <h3 class="chart-title">
              <LineChartOutlined />
              创作趋势
            </h3>
            <div ref="trendChartRef" class="chart-container"></div>
          </a-card>

          <!-- 性能统计图 -->
          <a-card :bordered="false" class="chart-card">
            <h3 class="chart-title">
              <ThunderboltOutlined />
              性能统计
            </h3>
            <div ref="performanceChartRef" class="chart-container performance-chart-container"></div>
          </a-card>
        </div>

        <!-- 用户统计 -->
        <div class="charts-grid">
          <a-card :bordered="false" class="chart-card">
            <h3 class="chart-title">
              <TeamOutlined />
              用户分析
            </h3>
            <div ref="userChartRef" class="chart-container"></div>
          </a-card>

          <a-card :bordered="false" class="chart-card">
            <h3 class="chart-title">
              <CrownOutlined />
              配额使用情况
            </h3>
            <div ref="quotaChartRef" class="chart-container"></div>
          </a-card>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  FileTextOutlined,
  BarChartOutlined,
  RiseOutlined,
  CheckCircleOutlined,
  LineChartOutlined,
  ThunderboltOutlined,
  TeamOutlined,
  CrownOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { getStatistics } from '@/api/statisticsController'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const loading = ref(false)
const stats = ref<API.StatisticsVO | null>(null)

// ECharts 实例
const trendChartRef = ref<HTMLElement>()
const performanceChartRef = ref<HTMLElement>()
const userChartRef = ref<HTMLElement>()
const quotaChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let performanceChart: echarts.ECharts | null = null
let userChart: echarts.ECharts | null = null
let quotaChart: echarts.ECharts | null = null

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getStatistics()
    stats.value = res.data.data || null

    // 渲染图表
    setTimeout(() => {
      renderTrendChart()
      renderPerformanceChart()
      renderUserChart()
      renderQuotaChart()
    }, 100)
  } catch (error) {
    message.error((error as Error).message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// 渲染创作趋势图
const renderTrendChart = () => {
  if (!trendChartRef.value || !stats.value) return

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const option: EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'line',
        lineStyle: { color: 'rgba(69, 111, 100, .35)' }
      },
      backgroundColor: 'rgba(32, 59, 56, .92)',
      borderWidth: 0,
      textStyle: { color: '#f7f5ee' },
      padding: [10, 14]
    },
    grid: {
      left: '2%',
      right: '2%',
      top: '9%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['今日', '本周', '本月', '总计'],
      boundaryGap: false,
      axisLine: {
        lineStyle: {
          color: 'rgba(69, 111, 100, .16)'
        }
      },
      axisLabel: {
        color: '#64766f',
        margin: 14
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(69, 111, 100, .1)',
          type: 'dashed'
        }
      },
      axisLabel: {
        color: '#64766f'
      }
    },
    series: [
      {
        name: '创作数量',
        type: 'line',
        smooth: true,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 9,
        data: [
          stats.value.todayCount ?? 0,
          stats.value.weekCount ?? 0,
          stats.value.monthCount ?? 0,
          stats.value.totalCount ?? 0
        ],
        itemStyle: {
          color: '#456f64',
          borderColor: '#f7f5ee',
          borderWidth: 3
        },
        lineStyle: {
          color: '#456f64',
          width: 3,
          shadowColor: 'rgba(69, 111, 100, .22)',
          shadowBlur: 10
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(143, 184, 164, .48)' },
            { offset: 1, color: 'rgba(143, 184, 164, .04)' }
          ])
        },
        emphasis: {
          scale: true,
          itemStyle: { color: '#c7a878' }
        },
        animationDuration: 1200,
        animationEasing: 'cubicOut'
      }
    ]
  }

  trendChart.setOption(option)
}

// 渲染性能统计图
const renderPerformanceChart = () => {
  if (!performanceChartRef.value || !stats.value) return

  if (!performanceChart) {
    performanceChart = echarts.init(performanceChartRef.value)
  }

  const durationMs = Math.max(0, stats.value.avgDurationMs ?? 0)
  const totalCount = Math.max(0, stats.value.totalCount ?? 0)
  const durationMax = Math.max(durationMs * 1.2, 1000)
  const countMax = Math.max(totalCount * 1.2, 10)

  const axisStyle = {
    axisLine: { show: false },
    axisTick: { show: false },
    splitLine: {
      lineStyle: {
        color: 'rgba(69, 111, 100, .1)',
        type: 'dashed' as const
      }
    },
    axisLabel: { color: '#64766f', fontSize: 11 }
  }

  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const item = (Array.isArray(params) ? params[0] : params) as {
          seriesName?: unknown
          value?: unknown
        }
        const value = Number(item.value ?? 0)
        return `${String(item.seriesName ?? '')}<br/><strong>${item.seriesName === '平均耗时' ? formatDuration(value) : `${value} 次`}</strong>`
      },
      backgroundColor: 'rgba(32, 59, 56, .92)',
      borderWidth: 0,
      textStyle: { color: '#f7f5ee' },
      padding: [10, 14]
    },
    grid: [
      { left: '18%', right: '12%', top: '10%', height: '29%', containLabel: true },
      { left: '18%', right: '12%', top: '60%', height: '29%', containLabel: true }
    ],
    xAxis: [
      {
        type: 'value',
        max: durationMax,
        gridIndex: 0,
        axisLabel: {
          ...axisStyle.axisLabel,
          formatter: (value: number) => formatDuration(value)
        },
        axisLine: axisStyle.axisLine,
        axisTick: axisStyle.axisTick,
        splitLine: axisStyle.splitLine
      },
      {
        type: 'value',
        max: countMax,
        gridIndex: 1,
        axisLabel: {
          ...axisStyle.axisLabel,
          formatter: (value: number) => `${value}`
        },
        axisLine: axisStyle.axisLine,
        axisTick: axisStyle.axisTick,
        splitLine: axisStyle.splitLine
      }
    ],
    yAxis: [
      {
        type: 'category',
        data: ['平均耗时'],
        gridIndex: 0,
        axisLabel: { color: '#64766f', fontSize: 13 },
        axisLine: axisStyle.axisLine,
        axisTick: axisStyle.axisTick
      },
      {
        type: 'category',
        data: ['总创作数'],
        gridIndex: 1,
        axisLabel: { color: '#64766f', fontSize: 13 },
        axisLine: axisStyle.axisLine,
        axisTick: axisStyle.axisTick
      }
    ],
    series: [
      {
        name: '平均耗时',
        type: 'bar',
        xAxisIndex: 0,
        yAxisIndex: 0,
        data: [durationMs],
        barWidth: 22,
        showBackground: true,
        backgroundStyle: { color: 'rgba(143, 184, 164, .12)', borderRadius: 12 },
        itemStyle: { color: '#456f64', borderRadius: [0, 12, 12, 0] },
        label: {
          show: true,
          position: 'right',
          color: '#203b38',
          fontSize: 14,
          fontWeight: 700,
          formatter: () => formatDuration(durationMs)
        }
      },
      {
        name: '总创作数',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: [totalCount],
        barWidth: 22,
        showBackground: true,
        backgroundStyle: { color: 'rgba(199, 168, 120, .14)', borderRadius: 12 },
        itemStyle: { color: '#c7a878', borderRadius: [0, 12, 12, 0] },
        label: {
          show: true,
          position: 'right',
          color: '#203b38',
          fontSize: 14,
          fontWeight: 700,
          formatter: () => `${totalCount} 次`
        }
      }
    ],
    animationDuration: 900,
    animationEasing: 'cubicOut'
  }

  performanceChart.setOption(option)
}

// 渲染用户分析图
const renderUserChart = () => {
  if (!userChartRef.value || !stats.value) return

  if (!userChart) {
    userChart = echarts.init(userChartRef.value)
  }

  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/><strong>{c}</strong> 位 · {d}%',
      backgroundColor: 'rgba(32, 59, 56, .92)',
      borderWidth: 0,
      textStyle: { color: '#f7f5ee' },
      padding: [10, 14]
    },
    legend: {
      orient: 'horizontal',
      left: 'center',
      bottom: 2,
      itemGap: 18,
      textStyle: {
        color: '#64766f',
        fontSize: 12
      }
    },
    graphic: [{
      type: 'text',
      left: '42%',
      top: '44%',
      style: {
        text: `${stats.value.totalUserCount ?? 0}`,
        fill: '#203b38',
        fontSize: 28,
        fontWeight: 700,
        textAlign: 'center',
        textVerticalAlign: 'middle'
      } as any
    }, {
      type: 'text',
      left: '42%',
      top: '56%',
      style: {
        text: '用户总数',
        fill: '#71817a',
        fontSize: 12,
        textAlign: 'center',
        textVerticalAlign: 'middle'
      } as any
    }],
    series: [
      {
        name: '用户分布',
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['42%', '46%'],
      avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 10,
          borderColor: 'rgba(247, 250, 246, .92)',
          borderWidth: 3
        },
        label: {
          show: false,
          position: 'outside',
          alignTo: 'labelLine',
          edgeDistance: 12,
          distanceToLabelLine: 6,
          bleedMargin: 8
        },
        emphasis: {
          label: {
            show: true,
            position: 'outside',
            alignTo: 'labelLine',
            edgeDistance: 12,
            distanceToLabelLine: 6,
            formatter: (params: unknown) => {
              const item = (Array.isArray(params) ? params[0] : params) as {
                name?: unknown
                value?: unknown
              }
              return `{name|${String(item.name ?? '')}}\n{count|${String(item.value ?? 0)} 位}`
            },
            rich: {
              name: {
                color: '#203b38',
                fontSize: 20,
                fontWeight: 700,
                lineHeight: 30
              },
              count: {
                color: '#71817a',
                fontSize: 14,
                lineHeight: 22
              }
            }
          },
          labelLine: {
            show: true,
            length: 22,
            length2: 42,
            smooth: true,
            lineStyle: {
              color: 'rgba(69, 111, 100, .38)',
              width: 1
            }
          },
          scale: false
        },
        labelLine: {
          show: false,
          length: 22,
          length2: 42,
          lineStyle: {
            color: 'rgba(69, 111, 100, .34)',
            width: 1
          }
        },
        data: [
          {
            value: stats.value.vipUserCount ?? 0,
            name: 'VIP 会员',
            itemStyle: { color: '#456f64' }
          },
          {
            value: stats.value.activeUserCount ?? 0,
            name: '活跃用户',
            itemStyle: { color: '#c7a878' }
          },
          {
            value: (stats.value.totalUserCount ?? 0) - (stats.value.activeUserCount ?? 0) - (stats.value.vipUserCount ?? 0),
            name: '其他用户',
            itemStyle: { color: '#b9c9c2' }
          }
        ],
        animationDuration: 1100,
        animationEasing: 'cubicOut'
      }
    ]
  }

  userChart.setOption(option)
}

// 渲染配额使用图
const renderQuotaChart = () => {
  if (!quotaChartRef.value || !stats.value) return

  if (!quotaChart) {
    quotaChart = echarts.init(quotaChartRef.value)
  }

  const normalUserCount = Math.max(0, (stats.value.totalUserCount ?? 0) - (stats.value.vipUserCount ?? 0))
  const totalQuota = normalUserCount * 5
  const usedQuota = stats.value.quotaUsed ?? 0
  const remainingQuota = Math.max(0, totalQuota - usedQuota)
  const hasVipUsers = (stats.value.vipUserCount ?? 0) > 0
  const quotaCenterValue = hasVipUsers ? '∞' : `${usedQuota}`
  const quotaCenterLabel = hasVipUsers ? 'VIP 无限配额' : '已使用配额'
  const quotaData = totalQuota > 0
    ? [
        { value: usedQuota, name: '已使用', itemStyle: { color: '#c78b7d' } },
        { value: remainingQuota, name: '剩余', itemStyle: { color: '#8fb8a4' } }
      ]
    : [{ value: 1, name: '暂无配额', itemStyle: { color: '#dfe9e3' } }]

  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const item = (Array.isArray(params) ? params[0] : params) as {
          name?: unknown
          value?: unknown
          percent?: unknown
        }
        return item.name === '暂无配额'
          ? '暂无可用配额'
          : `${String(item.name ?? '')}<br/><strong>${String(item.value ?? 0)}</strong> 次 · ${String(item.percent ?? 0)}%`
      },
      backgroundColor: 'rgba(32, 59, 56, .92)',
      borderWidth: 0,
      textStyle: { color: '#f7f5ee' },
      padding: [10, 14]
    },
    legend: {
      bottom: 4,
      left: 'center',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 20,
      textStyle: { color: '#64766f', fontSize: 12 }
    },
    series: [
      {
        name: '配额统计',
        type: 'pie',
        radius: ['54%', '74%'],
        center: ['50%', '50%'],
        data: quotaData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          show: true,
          position: 'center',
          formatter: `{value|${quotaCenterValue}}\n{label|${quotaCenterLabel}}`,
          rich: {
            value: {
              color: '#203b38',
              fontSize: 28,
              fontWeight: 700,
              lineHeight: 38
            },
            label: {
              color: '#71817a',
              fontSize: 12,
              lineHeight: 20
            },
          }
        },
        animationDuration: 1100,
        animationEasing: 'cubicOut'
      }
    ]
  }

  quotaChart.setOption(option)
}

// 格式化耗时
const formatDuration = (ms: number) => {
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

// 响应式处理
const handleResize = () => {
  trendChart?.resize()
  performanceChart?.resize()
  userChart?.resize()
  quotaChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  performanceChart?.dispose()
  userChart?.dispose()
  quotaChart?.dispose()
})
</script>

<style scoped lang="scss">
.statistics-page {
  background: var(--color-background-secondary);
  min-height: calc(100vh - 64px);
  padding-bottom: 60px;
  background-image:
    linear-gradient(155deg, rgba(231, 240, 234, .74), rgba(247, 245, 238, .9)),
    url('@/assets/scenes/admin-statistics.png');
  background-position: center top;
  background-size: cover;
  background-attachment: fixed;

  .page-header {
    background: var(--gradient-hero);
    padding: 32px 20px;
    margin-bottom: 24px;
  }

  .header-container {
    max-width: 1400px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-content {
    color: var(--color-text);
  }

  .page-title {
    font-size: 28px;
    font-weight: 700;
    margin: 0 0 6px;
    letter-spacing: -0.5px;
    color: var(--color-text);
  }

  .page-subtitle {
    font-size: 14px;
    color: var(--color-text-secondary);
    margin: 0;
  }

  .refresh-btn {
    height: 38px;
    border-radius: var(--radius-md);
    font-weight: 500;
  }

  .container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 20px;
  }

  /* 统计卡片网格 */
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 24px;
  }

  .stat-card {
    background: white;
    border-radius: var(--radius-lg);
    padding: 24px;
    border: 1px solid var(--color-border);
    display: flex;
    align-items: center;
    gap: 16px;
    transition: all var(--transition-normal);

    &:hover {
      box-shadow: var(--shadow-card-hover);
      transform: translateY(-2px);
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: var(--radius-lg);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      .anticon {
        font-size: 24px;
      }
    }

    .stat-content {
      flex: 1;
    }

    .stat-label {
      font-size: 13px;
      color: var(--color-text-secondary);
      margin-bottom: 6px;
      display: block;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: var(--color-text);
      line-height: 1;
    }
  }

  /* 图表网格 */
  .charts-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
    margin-bottom: 24px;
  }

  .chart-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    overflow: hidden;

    :deep(.ant-card-body) {
      padding: 24px;
    }

    .chart-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      margin: 0 0 20px;
      color: var(--color-text);

      .anticon {
        color: var(--color-primary);
        font-size: 18px;
      }
    }

    .chart-container {
      width: 100%;
      height: 300px;
    }

    .performance-stats {
      padding: 20px 0;

      .perf-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 0;

        .perf-label {
          font-size: 14px;
          color: var(--color-text-secondary);
        }

        .perf-value {
          font-size: 24px;
          font-weight: 600;
          color: var(--color-primary);
        }
      }
    }
  }

  /* 响应式 */
  @media (max-width: 1200px) {
    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .charts-grid {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 768px) {
    .page-header {
      padding: 24px 16px;
    }

    .header-container {
      flex-direction: column;
      gap: 16px;
      align-items: stretch;
    }

    .refresh-btn {
      width: 100%;
    }

    .stats-grid {
      grid-template-columns: 1fr;
      gap: 12px;
    }

    .stat-card {
      padding: 20px;
    }

    .chart-card {
      :deep(.ant-card-body) {
        padding: 20px;
      }

      .chart-container {
        height: 250px;
      }
    }
  }
}
/* 沅水青山运营视图：让关键数据先被看见，再进入图表细节 */
.statistics-page {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 82% 6%, rgba(199, 168, 120, .15), transparent 23%),
    linear-gradient(155deg, #e7f0ea 0%, #f7f5ee 48%, #eef5f0 100%);
}

.statistics-page .page-header {
  position: relative;
  padding: 42px 20px 36px;
  background: linear-gradient(110deg, rgba(32, 59, 56, .95), rgba(69, 111, 100, .88));
}

.statistics-page .page-header::after {
  position: absolute;
  right: 8%;
  bottom: -40px;
  width: 300px;
  height: 100px;
  border: 1px solid rgba(255, 255, 255, .2);
  border-radius: 50%;
  content: '';
  transform: rotate(-8deg);
}

.statistics-page .page-title { color: #f7f5ee; font-size: 34px; letter-spacing: -.04em; }
.statistics-page .page-subtitle { color: rgba(247, 245, 238, .7); }
.statistics-page .refresh-btn { border: 1px solid rgba(255, 255, 255, .34); color: #f7f5ee; background: rgba(255, 255, 255, .1); border-radius: 999px; }
.statistics-page .refresh-btn:hover { color: #fff; border-color: rgba(255, 255, 255, .7); background: rgba(255, 255, 255, .18); }
.statistics-page .container { position: relative; z-index: 1; }
.statistics-page .stat-card, .statistics-page .chart-card { border: 1px solid rgba(255, 255, 255, .76); background: rgba(247, 250, 246, .8); box-shadow: 0 22px 58px rgba(32, 59, 56, .08), inset 0 1px 0 rgba(255, 255, 255, .72); backdrop-filter: blur(16px) saturate(112%); -webkit-backdrop-filter: blur(16px) saturate(112%); }
.statistics-page .stat-card { position: relative; overflow: hidden; }
.statistics-page .stat-card::after { position: absolute; right: -22px; bottom: -28px; width: 96px; height: 56px; border: 1px solid rgba(69, 111, 100, .16); border-radius: 50%; content: ''; transform: rotate(-14deg); }
.statistics-page .stat-card:hover { box-shadow: 0 26px 64px rgba(32, 59, 56, .13); }
.statistics-page .stat-icon { background: rgba(143, 184, 164, .18) !important; }
.statistics-page .stat-icon .anticon { color: var(--mountain-green) !important; }
.statistics-page .stat-label, .statistics-page .perf-label { color: var(--ink-muted); }
.statistics-page .stat-value, .statistics-page .perf-value { color: var(--ink-deep); }
.statistics-page .chart-card { border-radius: 18px; }
.statistics-page .chart-card { position: relative; overflow: hidden; }
.statistics-page .chart-card::before {
  position: absolute;
  top: 0;
  right: 24px;
  left: 24px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(143, 184, 164, .72), rgba(199, 168, 120, .58), transparent);
  content: '';
}
.statistics-page .chart-title { color: var(--ink-deep); }
.statistics-page .chart-title .anticon { color: var(--mountain-green); }
.statistics-page .performance-stats { padding: 14px 4px; }
.statistics-page .performance-stats :deep(.ant-divider) { border-color: var(--line-soft); }
.statistics-page .chart-container { min-height: 300px; }
.statistics-page .performance-stats {
  display: flex;
  flex-direction: column;
  min-height: 300px;
  justify-content: center;
}
.statistics-page .performance-stats .perf-item {
  padding: 20px 22px;
  margin: 0 12px;
  border: 1px solid rgba(143, 184, 164, .22);
  border-radius: 14px;
  background: rgba(255, 255, 255, .42);
}
.statistics-page .performance-stats .perf-value {
  font-size: 32px;
  letter-spacing: -.02em;
}
.statistics-page .performance-stats :deep(.ant-divider) { margin: 18px 28px; }
.statistics-page .perf-summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 16px;
}
.statistics-page .perf-summary > div {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px 14px;
  border-left: 2px solid rgba(199, 168, 120, .72);
  background: rgba(255, 255, 255, .32);
}
.statistics-page .summary-label {
  color: var(--ink-muted);
  font-size: 12px;
}
.statistics-page .perf-summary strong {
  color: var(--ink-deep);
  font-size: 20px;
}
.statistics-page .chart-card :deep(.echarts) { filter: drop-shadow(0 10px 18px rgba(32, 59, 56, .05)); }

.statistics-page {
  background-image:
    linear-gradient(155deg, rgba(231, 240, 234, .34), rgba(247, 245, 238, .52)),
    url('@/assets/scenes/admin-statistics.png');
  background-position: center top;
  background-size: cover;
  background-attachment: fixed;
}

@media (max-width: 768px) {
  .statistics-page { background-attachment: scroll; }
  .statistics-page .page-header { padding: 32px 16px 28px; }
  .statistics-page .page-title { font-size: 28px; }
  .statistics-page .performance-stats { min-height: 250px; }
  .statistics-page .perf-summary { padding: 12px 0; }
}

.statistics-page .page-header {
  background: linear-gradient(135deg, rgba(224, 236, 229, .88), rgba(247, 245, 238, .72));
  color: var(--ink-deep);
  border-bottom: 1px solid var(--line-soft);
}

.statistics-page .page-header::after {
  border-color: rgba(69, 111, 100, .14);
}

.statistics-page .page-title { color: var(--ink-deep); }
.statistics-page .page-subtitle { color: var(--ink-muted); }
.statistics-page .refresh-btn {
  color: var(--ink-deep);
  border-color: rgba(69, 111, 100, .2);
  background: rgba(255, 255, 255, .45);
}
.statistics-page .refresh-btn:hover {
  color: var(--mountain-green);
  border-color: rgba(69, 111, 100, .42);
  background: rgba(255, 255, 255, .72);
}
</style>
