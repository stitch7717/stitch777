<template>
  <el-card shadow="never">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ================= 门诊收费 ================= -->
      <el-tab-pane label="门诊收费" name="charge">
        <el-table v-loading="pendingLoading" :data="pendingList" border stripe>
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="就诊号" prop="regNo" min-width="160" />
          <el-table-column label="患者" prop="patientName" min-width="100" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="['info', 'warning', 'success', 'danger'][row.status]" size="small">
                {{ ['', '就诊中', '已完成', '已退号'][row.status] || '候诊' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="挂号时间" prop="regTime" min-width="160" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openCharge(row)">去收费</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ================= 收费记录 ================= -->
      <el-tab-pane label="收费记录" name="records">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="支付状态">
            <el-select v-model="recordStatus" style="width: 140px" @change="loadRecords">
              <el-option label="全部" :value="null" />
              <el-option label="已支付" :value="1" />
              <el-option label="已退费" :value="2" />
            </el-select>
          </el-form-item>
        </el-form>
        <el-table v-loading="recordsLoading" :data="records" border stripe>
          <el-table-column label="收费单号" prop="chargeNo" min-width="160" />
          <el-table-column label="患者" prop="patientName" min-width="90" />
          <el-table-column label="实收" width="100" align="center">
            <template #default="{ row }">￥{{ row.actualAmount }}</template>
          </el-table-column>
          <el-table-column label="支付方式" width="90" align="center">
            <template #default="{ row }">{{ payTypeText(row.payType) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.payStatus === 1 ? 'success' : 'danger'" size="small">
                {{ row.payStatus === 1 ? '已支付' : '已退费' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="明细" min-width="240">
            <template #default="{ row }">
              <div v-for="it in row.items" :key="it.id" class="item-line">
                {{ it.itemName }} ￥{{ it.amount }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="收费时间" prop="chargeTime" min-width="160" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                :disabled="row.payStatus !== 1"
                @click="doInvoice(row)"
              >开发票</el-button>
              <el-button
                link
                type="danger"
                :disabled="row.payStatus !== 1"
                @click="doRefund(row)"
              >退费</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="recordPage"
          :total="recordTotal"
          layout="total, prev, pager, next"
          class="pager"
          @current-change="loadRecords"
        />
      </el-tab-pane>

      <!-- ================= 日结 ================= -->
      <el-tab-pane label="日结" name="daily">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="日期">
            <el-date-picker
              v-model="dailyDate"
              type="date"
              value-format="YYYY-MM-DD"
              @change="loadDaily"
            />
          </el-form-item>
        </el-form>
        <el-descriptions v-if="daily" :column="3" border>
          <el-descriptions-item label="日期">{{ daily.date }}</el-descriptions-item>
          <el-descriptions-item label="收费笔数">{{ daily.chargeCount }}</el-descriptions-item>
          <el-descriptions-item label="总收入（元）">
            <b style="color: #f56c6c; font-size: 16px">￥{{ daily.income }}</b>
          </el-descriptions-item>
          <el-descriptions-item label="退款（元）">￥{{ daily.refund }}</el-descriptions-item>
          <el-descriptions-item label="现金（元）">￥{{ daily.byPayType['1'] || 0 }}</el-descriptions-item>
          <el-descriptions-item label="微信（元）">￥{{ daily.byPayType['2'] || 0 }}</el-descriptions-item>
          <el-descriptions-item label="支付宝（元）">￥{{ daily.byPayType['3'] || 0 }}</el-descriptions-item>
          <el-descriptions-item label="医保（元）">￥{{ daily.byPayType['4'] || 0 }}</el-descriptions-item>
          <el-descriptions-item label="银行卡（元）">￥{{ daily.byPayType['5'] || 0 }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
    </el-tabs>

    <!-- 收费确认弹窗 -->
    <el-dialog title="收费确认" v-model="chargeDialogVisible" width="620px">
      <template v-if="pendingDetailData">
        <p style="margin-bottom: 10px">
          患者：<b>{{ pendingDetailData.patientName }}</b>
        </p>
        <el-table :data="pendingDetailData.items" border size="small">
          <el-table-column label="项目" prop="itemName" min-width="180" />
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">{{ ['', '药品', '诊疗', '检查', '床位'][row.itemType] }}</template>
          </el-table-column>
          <el-table-column label="金额" width="100" align="center">
            <template #default="{ row }">￥{{ row.amount }}</template>
          </el-table-column>
        </el-table>
        <div class="charge-total">
          合计：<b>￥{{ pendingDetailData.totalAmount }}</b>
        </div>
        <el-form label-width="80px" style="margin-top: 10px">
          <el-form-item label="支付方式">
            <el-radio-group v-model="chargePayType">
              <el-radio :value="1">现金</el-radio>
              <el-radio :value="2">微信</el-radio>
              <el-radio :value="3">支付宝</el-radio>
              <el-radio :value="4">医保</el-radio>
              <el-radio :value="5">银行卡</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="chargeDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="charging" @click="confirmCharge">确认收费</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPendingCharges,
  getPendingDetail,
  doCharge,
  refundCharge,
  getChargePage,
  getDailyReport,
  printInvoice
} from '@/api/system'

async function doInvoice(row) {
  const res = await printInvoice(row.id, 1)
  ElMessage.success(`发票已开具：${res.data.invoiceNo}`)
  loadRecords()
}

const activeTab = ref('charge')

// ---------- 待收费 ----------
const pendingLoading = ref(false)
const pendingList = ref([])

async function loadPending() {
  pendingLoading.value = true
  try {
    const res = await getPendingCharges()
    pendingList.value = res.data
  } finally {
    pendingLoading.value = false
  }
}

const chargeDialogVisible = ref(false)
const pendingDetailData = ref(null)
const chargePayType = ref(2)
const charging = ref(false)

async function openCharge(row) {
  const res = await getPendingDetail(row.visitId)
  pendingDetailData.value = res.data
  chargePayType.value = 2
  chargeDialogVisible.value = true
}

async function confirmCharge() {
  charging.value = true
  try {
    const res = await doCharge({
      visitId: pendingDetailData.value.visitId,
      payType: chargePayType.value
    })
    ElMessage.success(`收费成功：${res.data.chargeNo}，实收 ￥${res.data.actualAmount}`)
    chargeDialogVisible.value = false
    loadPending()
  } finally {
    charging.value = false
  }
}

// ---------- 收费记录 ----------
const recordsLoading = ref(false)
const records = ref([])
const recordTotal = ref(0)
const recordPage = ref(1)
const recordStatus = ref(null)

async function loadRecords() {
  recordsLoading.value = true
  try {
    const res = await getChargePage({
      pageNum: recordPage.value,
      pageSize: 10,
      payStatus: recordStatus.value
    })
    records.value = res.data.list
    recordTotal.value = res.data.total
  } finally {
    recordsLoading.value = false
  }
}

async function doRefund(row) {
  await ElMessageBox.confirm(
    `确认退费：${row.chargeNo}（￥${row.actualAmount}）？关联处方将转为已退。`,
    '提示',
    { type: 'warning' }
  )
  await refundCharge(row.id)
  ElMessage.success('退费成功')
  loadRecords()
}

function payTypeText(t) {
  return { 1: '现金', 2: '微信', 3: '支付宝', 4: '医保', 5: '银行卡' }[t] || '-'
}

// ---------- 日结 ----------
const dailyDate = ref(null)
const daily = ref(null)

async function loadDaily() {
  const res = await getDailyReport(dailyDate.value)
  daily.value = res.data
}

function onTabChange(tab) {
  if (tab === 'records') loadRecords()
  if (tab === 'daily') loadDaily()
}

onMounted(loadPending)
</script>

<style scoped>
.item-line {
  line-height: 1.7;
  font-size: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.charge-total {
  text-align: right;
  margin-top: 10px;
  font-size: 14px;
}

.charge-total b {
  color: #f56c6c;
  font-size: 18px;
}
</style>
