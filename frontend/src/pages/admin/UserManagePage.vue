<template>
  <div id="userManagePage">
    <WorkspacePageHeader title="用户管理" subtitle="管理系统中的所有用户" />

    <div class="container">
      <a-card :bordered="false" class="content-card">
        <!-- 搜索表单 -->
        <div class="search-section">
          <a-form layout="inline" :model="searchParams" @finish="doSearch" class="search-form">
            <a-form-item label="账号">
              <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" class="search-input" />
            </a-form-item>
            <a-form-item label="用户名">
              <a-input v-model:value="searchParams.userName" placeholder="输入用户名" class="search-input" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" class="search-btn">
                <template #icon>
                  <SearchOutlined />
                </template>
                搜索
              </a-button>
            </a-form-item>
          </a-form>
        </div>

        <a-divider />

        <div v-if="selectedRowKeys.length" class="batch-toolbar">
          <span class="selected-count">已选 {{ selectedRowKeys.length }} 位用户</span>
          <div class="batch-actions">
            <a-popconfirm :title="`确定删除选中的 ${selectedRowKeys.length} 位用户吗？`" ok-text="删除" cancel-text="取消" @confirm="deleteSelectedUsers">
              <a-button danger :loading="deleting"><DeleteOutlined /> 批量删除</a-button>
            </a-popconfirm>
            <a-button type="link" @click="clearSelection">取消选择</a-button>
          </div>
        </div>

        <!-- 表格 -->
        <a-table
          :columns="columns"
          :data-source="data"
          :pagination="pagination"
          :row-selection="rowSelection"
          row-key="id"
          :scroll="{ x: 1560 }"
          @change="doTableChange"
          class="user-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'userAvatar'">
              <a-avatar :src="record.userAvatar" :size="40" class="user-avatar" />
            </template>
            <template v-else-if="column.dataIndex === 'userRole'">
              <a-tag v-if="record.userRole === 'admin'" color="purple" class="role-tag">
                管理员
              </a-tag>
              <a-tag v-else-if="record.userRole === 'vip'" color="gold" class="role-tag">
                VIP 会员
              </a-tag>
              <a-tag v-else color="blue" class="role-tag">
                普通用户
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              <span class="time-text">{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-popconfirm
                title="确定要删除此用户吗?"
                ok-text="确定"
                cancel-text="取消"
                @confirm="doDelete(record.id)"
              >
                <a-button type="link" danger class="delete-btn">删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { batchDeleteUsers, deleteUser, listUserVoByPage } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { DeleteOutlined, SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import WorkspacePageHeader from '@/components/WorkspacePageHeader.vue'
import { useLoginUserStore } from '@/stores/loginUser'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 120,
    ellipsis: true,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    width: 110,
    ellipsis: true,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    width: 120,
    ellipsis: true,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 92,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    width: 150,
    ellipsis: true,
  },
  {
    title: 'QQ 邮箱',
    dataIndex: 'userEmail',
    width: 180,
    ellipsis: true,
  },
  {
    title: '电话',
    dataIndex: 'userPhone',
    width: 130,
    ellipsis: true,
  },
  {
    title: '博客',
    dataIndex: 'userBlog',
    width: 170,
    ellipsis: true,
  },
  {
    title: 'GitHub',
    dataIndex: 'userGithub',
    width: 170,
    ellipsis: true,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 170,
  },
  {
    title: '操作',
    key: 'action',
    width: 84,
  },
]

// 展示的数据
const data = ref<API.UserVO[]>([])
const total = ref(0)
type TableRowKey = string | number

const selectedRowKeys = ref<TableRowKey[]>([])
const deleting = ref(false)
const loginUserStore = useLoginUserStore()
const route = useRoute()
const router = useRouter()
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  getCheckboxProps: (record: API.UserVO) => ({ disabled: String(record.id) === String(loginUserStore.loginUser.id) }),
  onChange: (keys: TableRowKey[]) => {
    selectedRowKeys.value = keys
  },
}))

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const getQueryValue = (value: unknown) => Array.isArray(value) ? String(value[0] || '') : String(value || '')

const syncUserQuery = () => {
  const query: Record<string, string | string[]> = { ...route.query }
  delete query.userAccount
  delete query.userName
  delete query.page
  delete query.pageSize
  if (searchParams.userAccount?.trim()) query.userAccount = searchParams.userAccount.trim()
  if (searchParams.userName?.trim()) query.userName = searchParams.userName.trim()
  if ((searchParams.pageNum || 1) !== 1) query.page = String(searchParams.pageNum)
  if ((searchParams.pageSize || 10) !== 10) query.pageSize = String(searchParams.pageSize)
  void router.replace({ query })
}

const restoreUserQuery = () => {
  searchParams.userAccount = getQueryValue(route.query.userAccount)
  searchParams.userName = getQueryValue(route.query.userName)
  const page = Number(getQueryValue(route.query.page))
  const pageSize = Number(getQueryValue(route.query.pageSize))
  if (Number.isInteger(page) && page > 0) searchParams.pageNum = page
  if ([10, 20, 50, 100].includes(pageSize)) searchParams.pageSize = pageSize
}

// 获取数据
const fetchData = async () => {
  const res = await listUserVoByPage({
    ...searchParams,
  })
  if (res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
    selectedRowKeys.value = []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格分页变化时的操作
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  syncUserQuery()
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  syncUserQuery()
  fetchData()
}

// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id: Number(id) })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}

const clearSelection = () => {
  selectedRowKeys.value = []
}

const deleteSelectedUsers = async () => {
  const currentUserId = loginUserStore.loginUser.id
  if (currentUserId && selectedRowKeys.value.some((id) => String(id) === String(currentUserId))) {
    message.error('不能删除当前登录的管理员账号')
    return
  }
  deleting.value = true
  try {
    const res = await batchDeleteUsers({ ids: selectedRowKeys.value.map(Number) })
    message.success(`已删除 ${res.data.data ?? 0} 位用户`)
    await fetchData()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '批量删除失败')
  } finally {
    deleting.value = false
  }
}

// 页面加载时请求一次
onMounted(() => {
  restoreUserQuery()
  fetchData()
})
</script>

<style scoped lang="scss">
#userManagePage {
  background: var(--color-background-secondary);
  min-height: 100vh;
  padding-bottom: 60px;
  background-image:
    linear-gradient(180deg, rgba(247, 250, 246, .72), rgba(247, 245, 238, .92)),
    url('@/assets/scenes/admin-users.webp');
  background-position: center top;
  background-size: cover;
  background-attachment: fixed;

  .page-header {
    background: var(--gradient-hero);
    padding: 32px 20px;
    margin-bottom: 24px;
  }

  .header-container {
    max-width: 1200px;
    margin: 0 auto;
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

  .container {
    max-width: 1600px;
    margin: 0 auto;
    padding: 0 16px;
  }

  .content-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    box-shadow: none;
    background: white;

    :deep(.ant-card-body) {
      padding: 18px;
    }
  }

  .search-section {
    margin-bottom: 8px;
  }

  .batch-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin: 0 0 18px;
    padding: 12px 14px;
    border: 1px solid rgba(69, 111, 100, .18);
    border-radius: var(--radius-md);
    background: rgba(224, 236, 229, .55);
  }

  .selected-count { color: var(--ink-deep); font-size: 14px; font-weight: 600; }
  .batch-actions { display: flex; align-items: center; gap: 8px; }

  .search-form {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: flex-end;

    :deep(.ant-form-item) {
      margin-bottom: 0;
    }

    :deep(.ant-form-item-label > label) {
      font-weight: 500;
      font-size: 13px;
      color: var(--color-text-secondary);
    }
  }

  .search-input {
    width: 180px;
    border-radius: var(--radius-md);

    &:hover {
      border-color: var(--color-primary-light);
    }

    &:focus {
      border-color: var(--color-primary);
      box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.1);
    }
  }

  .search-btn {
    border-radius: var(--radius-md);
    font-weight: 500;
    background: var(--gradient-primary) !important;
    border: none !important;
    color: white !important;
    box-shadow: var(--shadow-green) !important;
    transition: opacity var(--transition-normal) !important;

    &:hover,
    &:focus,
    &:active {
      background: var(--gradient-primary) !important;
      border: none !important;
      color: white !important;
      box-shadow: var(--shadow-green) !important;
      opacity: 0.92;
    }

    :deep(.ant-wave) {
      display: none;
    }
  }

  .user-table {
    :deep(.ant-table-selection-column .ant-checkbox-checked .ant-checkbox-inner) {
      border-color: var(--mountain-green);
      background: var(--mountain-green);
    }

    :deep(.ant-table-selection-column .ant-checkbox-checked .ant-checkbox-inner::after) {
      border-color: #fff;
    }

    :deep(.ant-table-selection-column .ant-checkbox-indeterminate .ant-checkbox-inner::after) {
      background: var(--mountain-green);
    }

    :deep(.ant-table-cell) {
      white-space: nowrap;
    }

    :deep(.ant-table-thead > tr > th) {
      background: var(--color-background-secondary);
      font-weight: 600;
      font-size: 13px;
      color: var(--color-text-secondary);
      border-bottom: 1px solid var(--color-border);
      padding: 10px 12px;
    }

    :deep(.ant-table-tbody > tr > td) {
      padding: 8px 12px;
      border-bottom: 1px solid var(--color-border-light);
    }

    :deep(.ant-table-tbody > tr:hover > td) {
      background: rgba(34, 197, 94, 0.02);
    }

    :deep(.ant-table-pagination) {
      margin: 12px 0 0;
    }
  }

  .user-avatar {
    border: 2px solid var(--color-border);
  }

  .role-tag {
    border-radius: var(--radius-full);
    font-weight: 500;
    font-size: 12px;
    padding: 2px 10px;
  }

  .time-text {
    color: var(--color-text-secondary);
    font-size: 13px;
  }

  .delete-btn {
    font-weight: 500;
    font-size: 13px;
    color: var(--color-error);
    padding: 4px 8px;

    &:hover {
      color: #DC2626;
    }
  }
}

@media (max-width: 768px) {
  #userManagePage {
    .page-header {
      padding: 24px 20px;
    }

    .page-title {
      font-size: 22px;
    }

    .search-form {
      flex-direction: column;
      align-items: stretch;

      :deep(.ant-form-item) {
        width: 100%;
      }
    }

    .search-input {
      width: 100%;
    }

    .batch-toolbar { align-items: flex-start; flex-direction: column; }
  }
}
  /* 沅水青山后台：清晰的检索带与可扫描的数据表 */
  #userManagePage {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 84% 5%, rgba(199, 168, 120, .12), transparent 25%),
    linear-gradient(150deg, rgba(231, 240, 234, .34) 0%, rgba(247, 245, 238, .38) 42%, rgba(243, 247, 243, .46) 100%),
    url('@/assets/scenes/admin-users.webp');
  background-position: center top;
  background-size: cover;
  background-attachment: fixed;

  .page-header {
    position: relative;
    padding: 42px 20px 34px;
    background: linear-gradient(110deg, rgba(32, 59, 56, .94), rgba(69, 111, 100, .88));
    color: #f7f5ee;
  }

  .page-header::after {
    position: absolute;
    right: 9%;
    bottom: -36px;
    width: 260px;
    height: 92px;
    border: 1px solid rgba(255, 255, 255, .18);
    border-radius: 50%;
    content: '';
    transform: rotate(-8deg);
  }

  .page-title { color: #f7f5ee; font-size: 34px; letter-spacing: -.04em; }
  .page-subtitle { color: rgba(247, 245, 238, .7); }
  .content-card {
    margin-top: 28px;
    border: 1px solid rgba(255, 255, 255, .72);
    border-radius: 18px;
    background: rgba(247, 250, 246, .78);
    box-shadow: 0 24px 70px rgba(32, 59, 56, .1), inset 0 1px 0 rgba(255, 255, 255, .74);
    backdrop-filter: blur(18px) saturate(115%);
    -webkit-backdrop-filter: blur(18px) saturate(115%);
  }

  .search-section { padding: 4px 0; }
  .search-form :deep(.ant-form-item-label > label) { color: var(--ink-muted); font-size: 12px; }
  .search-input { border-color: rgba(69, 111, 100, .18); background: rgba(255, 255, 255, .68); }
  .search-input:focus { box-shadow: 0 0 0 3px rgba(143, 184, 164, .2); }
  .search-btn { border: 0 !important; border-radius: 999px; background: var(--ink-deep) !important; box-shadow: 0 8px 18px rgba(32, 59, 56, .15) !important; }
  .search-btn:hover { background: var(--mountain-green) !important; }
  .user-table :deep(.ant-table) { background: transparent; }
  .user-table :deep(.ant-table-thead > tr > th) { background: rgba(224, 236, 229, .62); color: var(--ink-muted); border-bottom-color: rgba(69, 111, 100, .15); }
  .user-table :deep(.ant-table-tbody > tr > td) { border-bottom-color: rgba(69, 111, 100, .1); color: var(--ink-deep); }
  .user-table :deep(.ant-table-tbody > tr:hover > td) { background: rgba(224, 236, 229, .45); }
  .user-avatar { border-color: rgba(255, 255, 255, .9); box-shadow: 0 5px 14px rgba(32, 59, 56, .12); }
  .role-tag { border: 0; }
  .delete-btn { border-radius: 999px; }

  @media (max-width: 768px) {
    .page-header { padding: 32px 20px 28px; }
    .page-title { font-size: 28px; }
    .content-card { margin-top: 18px; }
  }

  .page-header {
    background: linear-gradient(135deg, rgba(224, 236, 229, .88), rgba(247, 245, 238, .72));
    color: var(--ink-deep);
    border-bottom: 1px solid var(--line-soft);
  }

  .page-header::after {
    border-color: rgba(69, 111, 100, .14);
  }

  .page-title { color: var(--ink-deep); }
  .page-subtitle { color: var(--ink-muted); }
  }
</style>
