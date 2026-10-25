<template>
  <el-card shadow="never">
    <el-tabs v-model="tab" @tab-change="onTab">
      <!-- 站内消息 -->
      <el-tab-pane label="站内消息" name="message">
        <el-row class="mb8">
          <el-radio-group v-model="msgType" size="small" @change="loadMessages">
            <el-radio-button :value="null">全部</el-radio-button>
            <el-radio-button :value="1">系统</el-radio-button>
            <el-radio-button :value="2">待办</el-radio-button>
          </el-radio-group>
          <el-button size="small" icon="Check" @click="doReadAll">全部已读</el-button>
        </el-row>
        <el-table v-loading="msgLoading" :data="messages" border stripe>
          <el-table-column label="" width="40" align="center">
            <template #default="{ row }">
              <el-badge is-dot :hidden="row.isRead === 1" type="danger" />
            </template>
          </el-table-column>
          <el-table-column label="标题" min-width="120">
            <template #default="{ row }">
              <span :class="{ unread: row.isRead === 0 }">{{ row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column label="内容" prop="content" min-width="260" />
          <el-table-column label="时间" prop="createTime" min-width="160" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button v-if="row.isRead === 0" link type="primary" @click="doRead(row)">已读</el-button>
              <el-tag v-else size="small" type="info">已读</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 公告管理 -->
      <el-tab-pane label="公告管理" name="notice">
        <el-row class="mb8">
          <el-button type="primary" icon="Plus" @click="openNotice">发布公告</el-button>
        </el-row>
        <el-table v-loading="noticeLoading" :data="notices" border stripe>
          <el-table-column label="标题" prop="title" min-width="160" />
          <el-table-column label="内容" prop="content" min-width="240" />
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.noticeType === 1 ? 'primary' : 'warning'">
                {{ row.noticeType === 1 ? '院内' : '科室' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布人" prop="publisherName" width="100" />
          <el-table-column label="发布时间" prop="publishTime" min-width="150" />
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="doDeleteNotice(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 短信记录 -->
      <el-tab-pane label="短信记录" name="sms">
        <el-row class="mb8">
          <el-button type="primary" icon="Promotion" @click="smsVisible = true">发送短信</el-button>
          <el-input v-model="smsPhone" placeholder="按手机号筛选" clearable style="width: 180px" @keyup.enter="loadSms" />
          <el-button icon="Search" @click="loadSms">查询</el-button>
          <span class="tip">模拟网关：发送直记成功</span>
        </el-row>
        <el-table v-loading="smsLoading" :data="smsList" border stripe>
          <el-table-column label="手机号" prop="phone" min-width="120" />
          <el-table-column label="模板" prop="templateCode" width="100" align="center" />
          <el-table-column label="内容" prop="content" min-width="240" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.sendStatus === 1 ? 'success' : 'danger'">
                {{ row.sendStatus === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发送时间" prop="sendTime" min-width="160" />
        </el-table>
      </el-tab-pane>

      <!-- 预警提醒 -->
      <el-tab-pane label="预警提醒" name="warning">
        <el-alert type="info" :closable="false" class="mb8">
          <template #title>实时聚合：低库存 / 近效期 / 未处理危急值</template>
        </el-alert>
        <el-table v-loading="warnLoading" :data="warnings" border stripe>
          <el-table-column label="类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="{ low: 'warning', expire: 'danger', critical: 'danger' }[row.type]">
                {{ { low: '低库存', expire: '近效期', critical: '危急值' }[row.type] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="标题" prop="title" min-width="160" />
          <el-table-column label="详情" prop="detail" min-width="220" />
          <el-table-column label="时间" prop="time" min-width="160" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 发布公告 -->
    <el-dialog title="发布公告" v-model="noticeVisible" width="480px">
      <el-form label-width="80px">
        <el-form-item label="标题" required><el-input v-model="noticeForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="noticeForm.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="noticeForm.noticeType">
            <el-radio :value="1">院内</el-radio>
            <el-radio :value="2">科室</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="noticeForm.noticeType === 2" label="科室" required>
          <el-select v-model="noticeForm.deptId" style="width: 100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitNotice">发 布</el-button>
      </template>
    </el-dialog>

    <!-- 发送短信 -->
    <el-dialog title="发送短信" v-model="smsVisible" width="440px">
      <el-form label-width="80px">
        <el-form-item label="手机号" required><el-input v-model="smsForm.phone" /></el-form-item>
        <el-form-item label="内容" required><el-input v-model="smsForm.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="smsVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitSms">发 送</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMessagePage, readMessage, readAllMessages, getNoticePage,
  publishNotice, deleteNotice, getWarningList, getReportDepts
} from '@/api/system'

const tab = ref('message')
const messages = ref([])
const msgLoading = ref(false)
const msgType = ref(null)
const notices = ref([])
const noticeLoading = ref(false)
const warnings = ref([])
const warnLoading = ref(false)
const smsVisible = ref(false)
const smsLoading = ref(false)
const smsList = ref([])
const smsPhone = ref('')
const smsForm = reactive({ phone: '', templateCode: 'MANUAL', content: '' })
const depts = ref([])

async function loadMessages() {
  msgLoading.value = true
  try {
    const res = await getMessagePage({ pageNum: 1, pageSize: 20, msgType: msgType.value })
    messages.value = res.data.list
  } finally {
    msgLoading.value = false
  }
}

async function doRead(row) {
  await readMessage(row.id)
  loadMessages()
}

async function doReadAll() {
  await readAllMessages()
  ElMessage.success('已全部标记已读')
  loadMessages()
}

async function loadNotices() {
  noticeLoading.value = true
  try {
    const res = await getNoticePage({ pageNum: 1, pageSize: 50 })
    notices.value = res.data.list
  } finally {
    noticeLoading.value = false
  }
}

async function loadWarnings() {
  warnLoading.value = true
  try {
    const res = await getWarningList()
    warnings.value = res.data
  } finally {
    warnLoading.value = false
  }
}

const noticeVisible = ref(false)
const noticeForm = reactive({ title: '', content: '', noticeType: 1, deptId: null })

function openNotice() {
  Object.assign(noticeForm, { title: '', content: '', noticeType: 1, deptId: null })
  noticeVisible.value = true
}

async function submitNotice() {
  if (!noticeForm.title) {
    ElMessage.warning('请输入标题')
    return
  }
  await publishNotice(noticeForm)
  ElMessage.success('公告已发布')
  noticeVisible.value = false
  loadNotices()
}

async function doDeleteNotice(row) {
  await ElMessageBox.confirm(`确认删除公告「${row.title}」？`, '提示', { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success('已删除')
  loadNotices()
}

function onTab(t) {
  if (t === 'notice') loadNotices()
  if (t === 'warning') loadWarnings()
  if (t === 'sms') loadSms()
}

// ---------- 短信 ----------
async function loadSms() {
  smsLoading.value = true
  try {
    const res = await getSmsPage({ pageNum: 1, pageSize: 50, phone: smsPhone.value })
    smsList.value = res.data.list
  } finally {
    smsLoading.value = false
  }
}

async function submitSms() {
  if (!smsForm.phone || !smsForm.content) {
    ElMessage.warning('请填写手机号与内容')
    return
  }
  await sendSms(smsForm)
  ElMessage.success('短信已发送（模拟网关）')
  smsVisible.value = false
  loadSms()
}

onMounted(async () => {
  loadMessages()
  getReportDepts().then((res) => (depts.value = res.data))
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.unread {
  font-weight: 700;
  color: #303133;
}
</style>
