<template>
  <el-card shadow="never">
    <el-tabs v-model="tab" @tab-change="onTab">
      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="operation">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="用户">
            <el-input v-model="opQuery.username" placeholder="用户名" clearable style="width: 130px" @keyup.enter="loadOp" />
          </el-form-item>
          <el-form-item label="模块">
            <el-select v-model="opQuery.module" clearable placeholder="全部" style="width: 130px" @change="loadOp">
              <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="opQuery.status" clearable placeholder="全部" style="width: 110px" @change="loadOp">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadOp">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="opLoading" :data="opLogs" border stripe>
          <el-table-column label="用户" prop="username" width="100" />
          <el-table-column label="模块" prop="module" width="100" align="center" />
          <el-table-column label="操作" prop="method" min-width="220" />
          <el-table-column label="IP" prop="ip" width="130" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="80" align="center">
            <template #default="{ row }">{{ row.costTime }}ms</template>
          </el-table-column>
          <el-table-column label="时间" prop="createTime" min-width="160" />
        </el-table>
        <el-pagination v-model:current-page="opQuery.pageNum" :total="opTotal" layout="total, prev, pager, next"
          class="pager" @current-change="loadOp" />
      </el-tab-pane>

      <!-- 登录日志 -->
      <el-tab-pane label="登录日志" name="login">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="用户">
            <el-input v-model="loginQuery.username" clearable style="width: 140px" @keyup.enter="loadLogin" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadLogin">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loginLoading" :data="loginLogs" border stripe>
          <el-table-column label="用户名" prop="username" width="120" />
          <el-table-column label="IP" prop="ip" width="140" />
          <el-table-column label="结果" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="信息" prop="msg" min-width="180" />
          <el-table-column label="登录时间" prop="loginTime" min-width="160" />
        </el-table>
        <el-pagination v-model:current-page="loginQuery.pageNum" :total="loginTotal" layout="total, prev, pager, next"
          class="pager" @current-change="loadLogin" />
      </el-tab-pane>

      <!-- 数据备份 -->
      <el-tab-pane label="数据备份" name="backup">
        <el-row class="mb8">
          <el-button type="primary" icon="Upload" :loading="backing" @click="doBackup">手动备份</el-button>
          <span class="tip">备份为全库 SQL 快照（排除备份记录表）；恢复为全库回滚，需谨慎</span>
        </el-row>
        <el-table v-loading="backupLoading" :data="backups" border stripe>
          <el-table-column label="备份名称" prop="backupName" min-width="220" />
          <el-table-column label="大小" width="110" align="center">
            <template #default="{ row }">{{ (row.backupSize / 1024).toFixed(1) }} KB</template>
          </el-table-column>
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">{{ row.backupType === 1 ? '手动' : '自动' }}</template>
          </el-table-column>
          <el-table-column label="时间" prop="createTime" min-width="160" />
          <el-table-column label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button link type="warning" @click="doRestore(row)">恢复</el-button>
              <el-button link type="danger" @click="doDeleteBackup(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 安全配置 -->
      <el-tab-pane label="安全配置" name="security">
        <el-form label-width="160px" style="max-width: 520px" v-if="configs.length">
          <el-form-item v-for="c in configs" :key="c.id" :label="c.configName">
            <el-switch
              v-if="['0', '1'].includes(c.configValue)"
              v-model="c.boolValue"
              :disabled="c.configKey === 'security.two-factor'"
            />
            <el-input-number
              v-else-if="c.configKey === 'security.fail-count' || c.configKey === 'security.session-timeout'"
              v-model="c.numValue" :min="1" :max="9999"
            />
            <el-input v-else v-model="c.configValue" :placeholder="c.configKey === 'security.ip-whitelist' ? '空不限制，逗号分隔' : ''" />
            <span class="key-tip">{{ c.configKey }}</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveConfig">保存配置</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getOperationLogs, getLoginLogs, getBackupPage, createBackup,
  restoreBackup, deleteBackup, getSecurityConfig, updateSecurityConfig, getOperationModules
} from '@/api/system'

const tab = ref('operation')

// ---------- 操作日志 ----------
const opLoading = ref(false)
const opLogs = ref([])
const opTotal = ref(0)
const modules = ref([])
const opQuery = reactive({ pageNum: 1, pageSize: 10, username: '', module: null, status: null })

async function loadOp() {
  opLoading.value = true
  try {
    const res = await getOperationLogs(opQuery)
    opLogs.value = res.data.list
    opTotal.value = res.data.total
  } finally {
    opLoading.value = false
  }
}

// ---------- 登录日志 ----------
const loginLoading = ref(false)
const loginLogs = ref([])
const loginTotal = ref(0)
const loginQuery = reactive({ pageNum: 1, pageSize: 10, username: '' })

async function loadLogin() {
  loginLoading.value = true
  try {
    const res = await getLoginLogs(loginQuery)
    loginLogs.value = res.data.list
    loginTotal.value = res.data.total
  } finally {
    loginLoading.value = false
  }
}

// ---------- 备份 ----------
const backupLoading = ref(false)
const backing = ref(false)
const backups = ref([])

async function loadBackups() {
  backupLoading.value = true
  try {
    const res = await getBackupPage({ pageNum: 1, pageSize: 50 })
    backups.value = res.data.list
  } finally {
    backupLoading.value = false
  }
}

async function doBackup() {
  backing.value = true
  try {
    const res = await createBackup()
    ElMessage.success(`备份完成：${res.data.file}`)
    loadBackups()
  } finally {
    backing.value = false
  }
}

async function doRestore(row) {
  await ElMessageBox.confirm(
    `恢复将把全库回滚到 ${row.backupName} 的状态，当前数据会被覆盖且不可撤销！确认继续？`,
    '高危操作',
    { type: 'error', confirmButtonText: '我知道风险，继续恢复' }
  )
  await restoreBackup(row.id)
  ElMessage.success('恢复完成')
}

async function doDeleteBackup(row) {
  await ElMessageBox.confirm(`确认删除备份 ${row.backupName}？`, '提示', { type: 'warning' })
  await deleteBackup(row.id)
  ElMessage.success('已删除')
  loadBackups()
}

// ---------- 安全配置 ----------
const configs = ref([])

async function loadConfig() {
  const res = await getSecurityConfig()
  configs.value = res.data.map((c) => ({
    ...c,
    boolValue: c.configValue === '1',
    numValue: parseInt(c.configValue) || 1
  }))
}

async function saveConfig() {
  const payload = {}
  configs.value.forEach((c) => {
    if (['0', '1'].includes(c.configValue)) payload[c.configKey] = c.boolValue ? '1' : '0'
    else if (c.configKey === 'security.fail-count' || c.configKey === 'security.session-timeout')
      payload[c.configKey] = String(c.numValue)
    else payload[c.configKey] = c.configValue
  })
  await updateSecurityConfig(payload)
  ElMessage.success('配置已保存')
}

function onTab(t) {
  if (t === 'operation') loadOp()
  if (t === 'login') loadLogin()
  if (t === 'backup') loadBackups()
  if (t === 'security') loadConfig()
}

onMounted(() => {
  loadOp()
  getOperationModules().then((res) => (modules.value = res.data))
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.tip {
  color: #a8abb2;
  font-size: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.key-tip {
  margin-left: 10px;
  color: #a8abb2;
  font-size: 11px;
}
</style>
