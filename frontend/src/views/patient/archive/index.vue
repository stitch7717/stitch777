<template>
  <el-card shadow="never">
    <!-- 查询表单 -->
    <el-form :inline="true" :model="queryParams" @submit.prevent>
      <el-form-item label="关键字">
        <el-input
          v-model="queryParams.keyword"
          placeholder="姓名/手机号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="性别">
        <el-select v-model="queryParams.gender" placeholder="全部" clearable style="width: 100px">
          <el-option label="男" :value="1" />
          <el-option label="女" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-select
          v-model="queryParams.tags"
          placeholder="全部"
          clearable
          style="width: 130px"
        >
          <el-option
            v-for="tag in tagDict"
            :key="tag.dictValue"
            :label="tag.dictLabel"
            :value="tag.dictValue"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="就诊卡">
        <el-select
          v-model="queryParams.cardStatus"
          placeholder="全部"
          clearable
          style="width: 120px"
        >
          <el-option label="正常" :value="1" />
          <el-option label="挂失" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row class="mb8">
      <el-button type="primary" icon="Plus" @click="openDialog()">新建患者</el-button>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="patientList" border stripe>
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="编号" prop="patientNo" width="90" align="center" />
      <el-table-column label="姓名" prop="name" min-width="90" />
      <el-table-column label="性别" width="70" align="center">
        <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
      </el-table-column>
      <el-table-column label="年龄" prop="age" width="70" align="center" />
      <el-table-column label="手机号" prop="phone" min-width="120" />
      <el-table-column label="血型" prop="bloodType" width="70" align="center" />
      <el-table-column label="就诊卡" width="140" align="center">
        <template #default="{ row }">
          {{ row.cardNo }}
          <el-tag v-if="row.cardStatus === 0" type="danger" size="small">挂失</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="140">
        <template #default="{ row }">
          <el-tag
            v-for="tag in splitTags(row.tags)"
            :key="tag"
            :color="tagColor(tag)"
            :style="tagStyle(tag)"
            size="small"
            effect="plain"
            class="tag-chip"
          >
            {{ tag }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="建档时间" prop="createTime" min-width="160" />
      <el-table-column
        label="操作"
        width="240"
        align="center"
        fixed="right"
        class-name="small-padding fixed-width"
      >
        <template #default="{ row }">
          <el-button link type="primary" icon="View" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-dropdown class="op-more" @command="(cmd) => handleRowCommand(cmd, row)">
            <el-button link type="primary">
              更多<el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="loss" :disabled="row.cardStatus === 0">
                  就诊卡挂失
                </el-dropdown-item>
                <el-dropdown-item command="reissue" :disabled="row.cardStatus === 1">
                  补办就诊卡
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>
                  <span style="color: var(--el-color-danger)">删除</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pager"
      @size-change="getList"
      @current-change="getList"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="form.id ? '编辑患者' : '新建患者'"
      v-model="dialogVisible"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="血型" prop="bloodType">
              <el-select v-model="form.bloodType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="t in ['A', 'B', 'O', 'AB']" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="住址" prop="address">
          <el-input v-model="form.address" placeholder="请输入住址" />
        </el-form-item>
        <el-form-item label="患者标签" prop="tags">
          <el-select
            v-model="form.tagList"
            multiple
            placeholder="可多选"
            style="width: 100%"
          >
            <el-option
              v-for="tag in tagDict"
              :key="tag.dictValue"
              :label="tag.dictLabel"
              :value="tag.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="过敏史" prop="allergyHistory">
          <el-input
            v-model="form.allergyHistory"
            type="textarea"
            :rows="2"
            placeholder="如：青霉素过敏"
          />
        </el-form-item>
        <el-form-item label="既往病史" prop="medicalHistory">
          <el-input v-model="form.medicalHistory" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="家族病史" prop="familyHistory">
          <el-input v-model="form.familyHistory" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="患者详情" size="480px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="患者编号">{{ detail.patientNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ detail.gender === 1 ? '男' : '女' }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ detail.birthDate }}</el-descriptions-item>
          <el-descriptions-item label="身份证号" :span="2">{{ detail.idCard }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
          <el-descriptions-item label="血型">{{ detail.bloodType }}</el-descriptions-item>
          <el-descriptions-item label="就诊卡">
            {{ detail.cardNo }}
            <el-tag v-if="detail.cardStatus === 0" type="danger" size="small">挂失</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="卡余额">￥{{ detail.cardBalance }}</el-descriptions-item>
          <el-descriptions-item label="住址" :span="2">{{ detail.address }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <el-tag
              v-for="tag in splitTags(detail.tags)"
              :key="tag"
              :color="tagColor(tag)"
              :style="tagStyle(tag)"
              size="small"
              effect="plain"
              class="tag-chip"
            >
              {{ tag }}
            </el-tag>
            <span v-if="!detail.tags">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="过敏史" :span="2">{{ detail.allergyHistory || '-' }}</el-descriptions-item>
          <el-descriptions-item label="既往病史" :span="2">{{ detail.medicalHistory || '-' }}</el-descriptions-item>
          <el-descriptions-item label="家族病史" :span="2">{{ detail.familyHistory || '-' }}</el-descriptions-item>
          <el-descriptions-item label="建档时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <!-- 随访登记 -->
    <el-dialog :title="`随访登记：${followPatient?.name || ''}`" v-model="followVisible" width="440px">
      <el-form label-width="90px">
        <el-form-item label="计划日期" required>
          <el-date-picker v-model="followForm.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="随访方式">
          <el-radio-group v-model="followForm.followType">
            <el-radio :value="1">电话</el-radio><el-radio :value="2">上门</el-radio><el-radio :value="3">短信</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="followForm.content" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitFollow">确 定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  getPatientPage,
  getPatientDetail,
  createPatient,
  updatePatient,
  deletePatient,
  lossPatientCard,
  reissuePatientCard,
  getDictData,
  createFollowUp,
  getPatientRecords
} from '@/api/system'

// 标签字典（三色分级：red/yellow/green）
const tagDict = ref([])

const loading = ref(false)
const patientList = ref([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  gender: null,
  cardStatus: null,
  tags: null
})

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  gender: 1,
  birthDate: null,
  idCard: '',
  phone: '',
  address: '',
  bloodType: null,
  allergyHistory: '',
  medicalHistory: '',
  familyHistory: '',
  tagList: []
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
}

const detailVisible = ref(false)
const detail = ref(null)

const tagColorMap = computed(() =>
  Object.fromEntries(tagDict.value.map((t) => [t.dictValue, t.color]))
)

function splitTags(tags) {
  return tags ? tags.split(',').filter(Boolean) : []
}

function tagColor(tag) {
  return tagColorMap.value[tag] || ''
}

function tagStyle(tag) {
  const colorMap = {
    red: { color: '#f56c6c', borderColor: '#fbc4c4' },
    yellow: { color: '#e6a23c', borderColor: '#f3d19e' },
    green: { color: '#67c23a', borderColor: '#b3e19d' }
  }
  const c = tagColorMap.value[tag]
  return c && colorMap[c]
    ? { color: colorMap[c].color, borderColor: colorMap[c].borderColor }
    : { color: '#909399', borderColor: '#dcdfe6' }
}

async function getList() {
  loading.value = true
  try {
    const res = await getPatientPage(queryParams)
    patientList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function handleReset() {
  Object.assign(queryParams, {
    pageNum: 1,
    keyword: '',
    gender: null,
    cardStatus: null,
    tags: null
  })
  getList()
}

function openDialog(row) {
  if (row) {
    Object.keys(form).forEach((k) => (form[k] = null))
    form.id = row.id
    getPatientDetail(row.id).then((res) => {
      const d = res.data
      Object.assign(form, {
        id: d.id,
        name: d.name,
        gender: d.gender,
        birthDate: d.birthDate,
        idCard: d.idCard,
        phone: d.phone,
        address: d.address,
        bloodType: d.bloodType,
        allergyHistory: d.allergyHistory,
        medicalHistory: d.medicalHistory,
        familyHistory: d.familyHistory,
        tagList: splitTags(d.tags)
      })
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      gender: 1,
      birthDate: null,
      idCard: '',
      phone: '',
      address: '',
      bloodType: null,
      allergyHistory: '',
      medicalHistory: '',
      familyHistory: '',
      tagList: []
    })
  }
  dialogVisible.value = true
}

function submitForm() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = { ...form, tags: form.tagList.join(',') }
      delete payload.tagList
      if (form.id) {
        await updatePatient(form.id, payload)
        ElMessage.success('修改成功')
      } else {
        await createPatient(payload)
        ElMessage.success('建档成功')
      }
      dialogVisible.value = false
      getList()
    } finally {
      saving.value = false
    }
  })
}

async function openDetail(row) {
  const res = await getPatientDetail(row.id)
  detail.value = { ...res.data, createTime: row.createTime }
  detailVisible.value = true
}

function handleRowCommand(cmd, row) {
  if (cmd === 'delete') {
    ElMessageBox.confirm(`是否确认删除患者「${row.name}」（${row.patientNo}）？`, '提示', {
      type: 'warning'
    })
      .then(() => deletePatient(row.id))
      .then(() => {
        ElMessage.success('删除成功')
        getList()
      })
      .catch(() => {})
  } else if (cmd === 'loss') {
    ElMessageBox.confirm(`是否确认挂失患者「${row.name}」的就诊卡（${row.cardNo}）？`, '提示', {
      type: 'warning'
    })
      .then(() => lossPatientCard(row.id))
      .then(() => {
        ElMessage.success('挂失成功')
        getList()
      })
      .catch(() => {})
  } else if (cmd === 'reissue') {
    reissuePatientCard(row.id).then((res) => {
      ElMessage.success(`补办成功，新卡号：${res.data.cardNo}`)
      getList()
    })
  } else if (cmd === 'followup') {
    followPatient.value = row
    Object.assign(followForm, { patientId: row.id, planDate: '', followType: 1, content: '' })
    followVisible.value = true
  } else if (cmd === 'records') {
    getPatientRecords(row.id).then((res) => {
      recordList.value = res.data
      ElMessageBox.alert(
        res.data.length
          ? res.data.map((r) => `${r.visitNo}｜${r.diagnosis || '无诊断'}｜${r.status === 1 ? '已提交' : '草稿'}`).join('<br/>')
          : '该患者暂无病历',
        `病历记录：${row.name}`,
        { dangerouslyUseHTMLString: true }
      )
    })
  }
}

// ---------- 随访 ----------
const followVisible = ref(false)
const followPatient = ref(null)
const followForm = reactive({ patientId: null, planDate: '', followType: 1, content: '' })
const recordList = ref([])

async function submitFollow() {
  if (!followForm.planDate || !followForm.content) {
    ElMessage.warning('请填写计划日期与内容')
    return
  }
  await createFollowUp(followForm)
  ElMessage.success('随访计划已登记')
  followVisible.value = false
}

onMounted(async () => {
  getList()
  const dictRes = await getDictData('patient_tag')
  tagDict.value = dictRes.data || []
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.tag-chip {
  margin-right: 6px;
  border-style: solid;
  background: #fff;
}

.op-more {
  margin-left: 12px;
  vertical-align: middle;
}
</style>
