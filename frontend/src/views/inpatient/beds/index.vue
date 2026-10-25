<template>
  <el-card shadow="never">
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="病区">
        <el-select v-model="wardId" placeholder="全部病区" clearable style="width: 160px" @change="loadBeds">
          <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Refresh" @click="loadBeds">刷新</el-button>
        <el-button type="success" icon="Plus" @click="openAdmit">入院登记</el-button>
      </el-form-item>
    </el-form>

    <!-- 床位卡片区 -->
    <div class="bed-grid">
      <div
        v-for="bed in beds"
        :key="bed.id"
        class="bed-card"
        :class="bedClass(bed.status)"
        @click="bed.status === 1 ? openDetail(bed) : null"
      >
        <div class="bed-no">{{ bed.bedNo }}</div>
        <div class="bed-patient">{{ bed.patientName || statusText(bed.status) }}</div>
        <div class="bed-meta">{{ wardName(bed.wardId) }} · ￥{{ bed.price }}/天</div>
      </div>
    </div>
    <div class="legend">
      <span class="dot free" />空闲
      <span class="dot occupied" />占用（点击查看详情）
      <span class="dot repair" />维修
    </div>

    <!-- 入院弹窗 -->
    <el-dialog title="入院登记" v-model="admitVisible" width="520px">
      <el-form label-width="90px">
        <el-form-item label="患者" required>
          <el-select v-model="admitForm.patientId" filterable remote :remote-method="searchPatients"
            :loading="searching" placeholder="输入姓名/手机号搜索" style="width: 100%">
            <el-option v-for="p in patientOptions" :key="p.id" :label="`${p.name}（${p.patientNo}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="病区" required>
          <el-select v-model="admitForm.deptId" style="width: 100%" @change="loadFreeBeds">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="床位" required>
          <el-select v-model="admitForm.bedId" style="width: 100%" placeholder="请先选病区">
            <el-option v-for="b in freeBeds" :key="b.id" :label="`${b.bedNo}（￥${b.price}/天）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="主治医生">
          <el-select v-model="admitForm.doctorId" clearable style="width: 100%">
            <el-option v-for="d in doctors" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任护士">
          <el-select v-model="admitForm.nurseId" clearable style="width: 100%">
            <el-option v-for="n in nurses" :key="n.id" :label="n.name" :value="n.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入院诊断">
          <el-input v-model="admitForm.diagnosis" />
        </el-form-item>
        <el-form-item label="押金">
          <el-input-number v-model="admitForm.deposit" :min="0" :step="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="admitVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdmit">确认入院</el-button>
      </template>
    </el-dialog>

    <!-- 住院详情抽屉 -->
    <el-drawer v-model="detailVisible" title="住院详情" size="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="住院号">{{ detail.admissionNo }}</el-descriptions-item>
          <el-descriptions-item label="床位">{{ detail.bedNo }}</el-descriptions-item>
          <el-descriptions-item label="患者">{{ detail.patientName }}</el-descriptions-item>
          <el-descriptions-item label="病区">{{ detail.deptName }}</el-descriptions-item>
          <el-descriptions-item label="主治">{{ detail.doctorName }}</el-descriptions-item>
          <el-descriptions-item label="护士">{{ detail.nurseName }}</el-descriptions-item>
          <el-descriptions-item label="入院" :span="2">{{ detail.admissionTime }}</el-descriptions-item>
          <el-descriptions-item label="诊断" :span="2">{{ detail.diagnosis }}</el-descriptions-item>
          <el-descriptions-item label="押金">￥{{ detail.deposit }}</el-descriptions-item>
          <el-descriptions-item label="在院天数">{{ detail.inDays }} 天</el-descriptions-item>
          <el-descriptions-item label="床位费估算">￥{{ detail.bedFeeEstimate }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="detail.status === 0 ? 'success' : 'info'">
              {{ ['', '已出院', '已转科'][detail.status] || '在院' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="op-bar" v-if="detail.status === 0">
          <el-button size="small" type="danger" @click="doSettle">出院结算</el-button>
          <el-button size="small" type="warning" @click="doDischarge">办理出院</el-button>
          <el-button size="small" @click="openTransfer">转科</el-button>
          <el-button size="small" type="primary" @click="orderVisible = true">开医嘱</el-button>
          <el-button size="small" type="success" @click="nursingVisible = true">护理记录</el-button>
        </div>

        <el-tabs v-model="detailTab">
          <el-tab-pane label="医嘱" name="orders">
            <el-table :data="detail.orders" size="small" border>
              <el-table-column label="内容" prop="content" min-width="140" />
              <el-table-column label="用法" prop="usageMethod" width="70" />
              <el-table-column label="频次" prop="frequency" width="80" />
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="['info', 'success', 'danger'][row.status]">
                    {{ ['未执行', '执行中', '已停止'][row.status] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="110" align="center">
                <template #default="{ row }">
                  <el-button link size="small" :disabled="row.status !== 0" @click="doExecute(row)">执行</el-button>
                  <el-button link size="small" type="danger" :disabled="row.status !== 1" @click="doStop(row)">停止</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 转科弹窗 -->
    <el-dialog title="转科" v-model="transferVisible" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="目标病区">
          <el-select v-model="transferForm.newDeptId" style="width: 100%" @change="loadFreeBeds">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标床位">
          <el-select v-model="transferForm.newBedId" style="width: 100%">
            <el-option v-for="b in freeBeds" :key="b.id" :label="b.bedNo" :value="b.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitTransfer">确认转科</el-button>
      </template>
    </el-dialog>

    <!-- 开医嘱 -->
    <el-dialog title="开医嘱" v-model="orderVisible" width="480px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="orderForm.orderType">
            <el-radio :value="1">长期</el-radio>
            <el-radio :value="2">临时</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="orderForm.content" placeholder="如：0.9%氯化钠 500ml" />
        </el-form-item>
        <el-form-item label="用法">
          <el-select v-model="orderForm.usageMethod" style="width: 100%">
            <el-option v-for="u in ['口服', '静滴', '肌注', '雾化']" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item label="频次">
          <el-select v-model="orderForm.frequency" style="width: 100%">
            <el-option v-for="f in ['每日1次', '每日2次', '每日3次', '必要时']" :key="f" :label="f" :value="f" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitOrder">提 交</el-button>
      </template>
    </el-dialog>

    <!-- 护理记录 -->
    <el-dialog title="护理记录" v-model="nursingVisible" width="480px" append-to-body>
      <el-form label-width="90px">
        <el-row :gutter="8">
          <el-col :span="6"><el-form-item label="体温℃"><el-input-number v-model="nursingForm.temperature" :min="30" :max="45" :precision="1" :controls="false" style="width: 80px" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="脉搏"><el-input-number v-model="nursingForm.pulse" :controls="false" style="width: 80px" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="呼吸"><el-input-number v-model="nursingForm.breath" :controls="false" style="width: 80px" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="血压"><el-input v-model="nursingForm.bloodPressure" placeholder="120/80" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="护理内容">
          <el-input v-model="nursingForm.content" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nursingVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitNursing">提 交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBeds, getFreeBeds, admitPatient, dischargeAdmission, transferAdmission,
  getAdmissionDetail, addOrder, executeOrder, stopOrder, addNursing,
  getInpatientDepts, getInpatientDoctors, getInpatientNurses, getPatientPage,
  settleAdmission
} from '@/api/system'

async function doSettle() {
  await ElMessageBox.confirm(
    `确认为「${detail.value.patientName}」办理出院结算？将生成住院收费单并办理出院。`,
    '出院结算', { type: 'warning' }
  )
  const res = await settleAdmission(detail.value.id)
  ElMessage.success(
    `结算完成：${res.data.chargeNo}，费用 ￥${res.data.total}，押金退返 ￥${res.data.refundable}`
  )
  detailVisible.value = false
  loadBeds()
}

const beds = ref([])
const depts = ref([])
const doctors = ref([])
const nurses = ref([])
const wardId = ref(null)

async function loadBeds() {
  const res = await getBeds({ wardId: wardId.value })
  beds.value = res.data
}

function bedClass(s) {
  return { free: s === 0, occupied: s === 1, repair: s === 2 }
}
function statusText(s) {
  return { 0: '空闲', 1: '占用', 2: '维修' }[s]
}
function wardName(id) {
  return depts.value.find((d) => d.id === id)?.name || ''
}

// ---------- 入院 ----------
const admitVisible = ref(false)
const saving = ref(false)
const freeBeds = ref([])
const patientOptions = ref([])
const searching = ref(false)
const admitForm = reactive({
  patientId: null, deptId: null, bedId: null, doctorId: null, nurseId: null, diagnosis: '', deposit: 3000
})

function openAdmit() {
  Object.assign(admitForm, { patientId: null, deptId: null, bedId: null, doctorId: null, nurseId: null, diagnosis: '', deposit: 3000 })
  admitVisible.value = true
}

async function searchPatients(kw) {
  if (!kw) return
  searching.value = true
  try {
    const res = await getPatientPage({ pageNum: 1, pageSize: 20, keyword: kw })
    patientOptions.value = res.data.list
  } finally {
    searching.value = false
  }
}

async function loadFreeBeds() {
  const res = await getFreeBeds(admitForm.deptId)
  freeBeds.value = res.data
}

async function submitAdmit() {
  if (!admitForm.patientId || !admitForm.deptId || !admitForm.bedId) {
    ElMessage.warning('请完整选择患者/病区/床位')
    return
  }
  saving.value = true
  try {
    const res = await admitPatient(admitForm)
    ElMessage.success(`入院成功：${res.data.admissionNo}`)
    admitVisible.value = false
    loadBeds()
  } finally {
    saving.value = false
  }
}

// ---------- 详情/出院/转科/医嘱/护理 ----------
const detailVisible = ref(false)
const detail = ref(null)
const detailTab = ref('orders')
const transferVisible = ref(false)
const orderVisible = ref(false)
const nursingVisible = ref(false)
const transferForm = reactive({ newDeptId: null, newBedId: null })
const orderForm = reactive({ orderType: 1, content: '', usageMethod: '口服', frequency: '每日3次' })
const nursingForm = reactive({ temperature: 36.5, pulse: 80, breath: 18, bloodPressure: '', content: '' })

async function openDetail(bed) {
  const res = await getAdmissionDetail(bed.admissionId)
  detail.value = res.data
  detailVisible.value = true
}

async function refreshDetail() {
  const res = await getAdmissionDetail(detail.value.id)
  detail.value = res.data
  loadBeds()
}

async function doDischarge() {
  await ElMessageBox.confirm(`确认给「${detail.value.patientName}」办理出院？`, '提示', { type: 'warning' })
  await dischargeAdmission(detail.value.id)
  ElMessage.success('已出院，床位已释放')
  detailVisible.value = false
  loadBeds()
}

function openTransfer() {
  transferForm.newDeptId = null
  transferForm.newBedId = null
  transferVisible.value = true
}

async function submitTransfer() {
  if (!transferForm.newDeptId || !transferForm.newBedId) {
    ElMessage.warning('请选择目标病区与床位')
    return
  }
  await transferAdmission(detail.value.id, transferForm)
  ElMessage.success('转科成功')
  transferVisible.value = false
  refreshDetail()
}

async function submitOrder() {
  if (!orderForm.content) {
    ElMessage.warning('请输入医嘱内容')
    return
  }
  await addOrder({ ...orderForm, admissionId: detail.value.id })
  ElMessage.success('医嘱已开立')
  orderVisible.value = false
  refreshDetail()
}

async function doExecute(row) {
  await executeOrder(row.id)
  ElMessage.success('已执行')
  refreshDetail()
}

async function doStop(row) {
  await stopOrder(row.id)
  ElMessage.success('已停止')
  refreshDetail()
}

async function submitNursing() {
  await addNursing({ ...nursingForm, admissionId: detail.value.id })
  ElMessage.success('护理记录已保存')
  nursingVisible.value = false
}

onMounted(async () => {
  loadBeds()
  const d = await getInpatientDepts()
  depts.value = d.data
  doctors.value = (await getInpatientDoctors()).data
  nurses.value = (await getInpatientNurses()).data
})

defineExpose({ openAdmit })
</script>

<style scoped>
.bed-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 110px);
  gap: 12px;
  margin: 10px 0;
}

.bed-card {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 10px;
  text-align: center;
  cursor: default;
}

.bed-card.free {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.bed-card.occupied {
  border-color: #1f6feb;
  background: #ecf5ff;
  cursor: pointer;
}

.bed-card.repair {
  border-color: #f3d19e;
  background: #fdf6ec;
  opacity: 0.7;
}

.bed-no {
  font-size: 16px;
  font-weight: 700;
}

.bed-patient {
  margin: 6px 0;
  font-size: 13px;
  color: #303133;
}

.bed-meta {
  font-size: 11px;
  color: #a8abb2;
}

.legend {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 12px;
  color: #606266;
}

.dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
  margin-right: 4px;
}

.dot.free {
  background: #f0f9eb;
  border: 1px solid #b3e19d;
}

.dot.occupied {
  background: #ecf5ff;
  border: 1px solid #1f6feb;
}

.dot.repair {
  background: #fdf6ec;
  border: 1px solid #f3d19e;
}

.op-bar {
  margin: 12px 0;
  display: flex;
  gap: 8px;
}
</style>
