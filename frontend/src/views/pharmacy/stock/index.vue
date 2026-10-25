<template>
  <el-card shadow="never">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ================= 药品库存 ================= -->
      <el-tab-pane label="药品库存" name="stock">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="名称/编码">
            <el-input
              v-model="queryParams.keyword"
              placeholder="药品名称/编码"
              clearable
              style="width: 200px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-row class="mb8">
          <el-button type="primary" icon="Plus" @click="openDrugDialog()">新增药品</el-button>
          <span class="warn-summary" v-if="warnCount">⚠ 库存预警 {{ warnCount }} 项</span>
        </el-row>

        <el-table v-loading="loading" :data="drugList" border stripe>
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="编码" prop="drugCode" width="90" align="center" />
          <el-table-column label="名称" prop="drugName" min-width="140">
            <template #default="{ row }">
              {{ row.drugName }}
              <el-tag v-if="row.isSpecial === 1" type="danger" size="small">特</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="规格" prop="spec" min-width="110" />
          <el-table-column label="库存" width="80" align="center">
            <template #default="{ row }">
              <span :class="{ 'low-qty': row.stockStatus === 'lowStock' }">{{ row.quantity }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预警" prop="warnNum" width="70" align="center" />
          <el-table-column label="有效期" prop="expireDate" width="110" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.stockStatus === 'nearExpire'" type="danger" size="small">近效期</el-tag>
              <el-tag v-else-if="row.stockStatus === 'lowStock'" type="warning" size="small">库存预警</el-tag>
              <el-tag v-else type="success" size="small">正常</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="售价" width="90" align="center">
            <template #default="{ row }">￥{{ row.salePrice }}</template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="220"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openStockDialog(row, 1)">入库</el-button>
              <el-button link type="warning" @click="openStockDialog(row, 2)">出库</el-button>
              <el-button link type="primary" @click="openDrugDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="doDeleteDrug(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          class="pager"
          @current-change="getList"
        />
      </el-tab-pane>

      <!-- ================= 门诊发药 ================= -->
      <el-tab-pane label="门诊发药" name="dispense">
        <el-radio-group v-model="dispenseStatus" @change="loadDispenseList" class="mb8">
          <el-radio-button :value="0">待审核</el-radio-button>
          <el-radio-button :value="2">待发药(已审核)</el-radio-button>
          <el-radio-button :value="3">已发药</el-radio-button>
          <el-radio-button :value="4">已退</el-radio-button>
        </el-radio-group>
        <el-table v-loading="dispenseLoading" :data="dispenseList" border stripe>
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="处方号" prop="prescNo" min-width="160" />
          <el-table-column label="金额" width="110" align="center">
            <template #default="{ row }">￥{{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="开方时间" prop="createTime" min-width="160" />
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 2"
                link
                type="primary"
                @click="doDispense(row)"
              >确认发药</el-button>
              <el-button
                v-if="row.status === 3"
                link
                type="danger"
                @click="doReturn(row)"
              >退药</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="采购管理" name="purchase">
        <el-row class="mb8">
          <el-button type="primary" icon="Plus" @click="openPurchase">新增采购</el-button>
          <el-radio-group v-model="purchaseStatus" size="small" @change="loadPurchase">
            <el-radio-button :value="0">待入库</el-radio-button>
            <el-radio-button :value="1">已入库</el-radio-button>
            <el-radio-button :value="2">已取消</el-radio-button>
          </el-radio-group>
        </el-row>
        <el-table v-loading="purchaseLoading" :data="purchaseList" border stripe>
          <el-table-column label="采购单号" prop="orderNo" min-width="150" />
          <el-table-column label="供应商" prop="supplierName" min-width="130" />
          <el-table-column label="药品" prop="drugName" min-width="120" />
          <el-table-column label="数量" prop="quantity" width="70" align="center" />
          <el-table-column label="单价" width="80" align="center">
            <template #default="{ row }">￥{{ row.unitPrice }}</template>
          </el-table-column>
          <el-table-column label="总金额" width="90" align="center">
            <template #default="{ row }">￥{{ row.total }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="['warning', 'success', 'info'][row.status]">
                {{ ['待入库', '已入库', '已取消'][row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button v-if="row.status === 0" link type="primary" @click="doReceive(row)">入库</el-button>
              <el-button v-if="row.status === 0" link type="danger" @click="doCancelPurchase(row)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ================= 预警 ================= -->
      <el-tab-pane label="库存预警" name="warn">
        <el-table v-loading="warnLoading" :data="warnList" border stripe>
          <el-table-column label="药品" prop="drugName" min-width="150" />
          <el-table-column label="当前库存" prop="quantity" width="100" align="center" />
          <el-table-column label="预警线" prop="warnNum" width="100" align="center" />
          <el-table-column label="有效期" prop="expireDate" width="120" align="center" />
          <el-table-column label="预警类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag type="danger" size="small" v-if="row.type === 'expire'">近效期</el-tag>
              <el-tag type="warning" size="small" v-else-if="row.type === 'lowStock'">低库存</el-tag>
              <el-tag type="danger" size="small" v-else>低库存+近效期</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑药品 -->
    <el-dialog :title="drugForm.id ? '编辑药品' : '新增药品'" v-model="drugDialogVisible" width="560px">
      <el-form ref="drugFormRef" :model="drugForm" :rules="drugRules" label-width="90px">
        <el-form-item label="药品名称" prop="drugName">
          <el-input v-model="drugForm.drugName" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="规格" prop="spec">
              <el-input v-model="drugForm.spec" placeholder="如 0.25g*24粒" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="剂型" prop="dosageForm">
              <el-input v-model="drugForm.dosageForm" placeholder="如 胶囊剂" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="售价" prop="salePrice">
              <el-input-number v-model="drugForm.salePrice" :min="0.01" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="进价" prop="purchasePrice">
              <el-input-number v-model="drugForm.purchasePrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="生产厂家">
          <el-input v-model="drugForm.manufacturer" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="drugForm.category" placeholder="如 抗生素" />
        </el-form-item>
        <el-form-item label="特殊药品">
          <el-radio-group v-model="drugForm.isSpecial">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drugDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitDrug">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 入库/出库 -->
    <el-dialog
      :title="stockForm.type === 1 ? '药品入库' : '药品出库'"
      v-model="stockDialogVisible"
      width="440px"
    >
      <el-form label-width="90px">
        <el-form-item label="药品">
          <el-input :model-value="stockForm.drugName" disabled />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="stockForm.quantity" :min="1" :max="10000" />
        </el-form-item>
        <template v-if="stockForm.type === 1">
          <el-form-item label="批号">
            <el-input v-model="stockForm.batchNo" placeholder="选填" />
          </el-form-item>
          <el-form-item label="生产日期">
            <el-date-picker v-model="stockForm.produceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="有效期">
            <el-date-picker v-model="stockForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </template>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitStock">确 定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDrugPage,
  createDrug,
  updateDrug,
  deleteDrug,
  stockIn,
  stockOut,
  getWarnList,
  dispense,
  returnDrug,
  getDispensePrescriptions,
  getPurchasePage, createPurchase, receivePurchase, cancelPurchase, getSuppliers
} from '@/api/system'

const activeTab = ref('stock')

// ---------- 药品库存 ----------
const loading = ref(false)
const drugList = ref([])
const total = ref(0)
const warnCount = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

async function getList() {
  loading.value = true
  try {
    const res = await getDrugPage(queryParams)
    drugList.value = res.data.list
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
  queryParams.pageNum = 1
  queryParams.keyword = ''
  getList()
}

// ---------- 新增/编辑药品 ----------
const drugDialogVisible = ref(false)
const drugFormRef = ref()
const drugForm = reactive({
  id: null,
  drugName: '',
  spec: '',
  dosageForm: '',
  salePrice: 1,
  purchasePrice: 0,
  manufacturer: '',
  category: '',
  isSpecial: 0
})
const drugRules = {
  drugName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

function openDrugDialog(row) {
  Object.assign(drugForm, {
    id: row?.id || null,
    drugName: row?.drugName || '',
    spec: row?.spec || '',
    dosageForm: row?.dosageForm || '',
    salePrice: row?.salePrice || 1,
    purchasePrice: row?.purchasePrice || 0,
    manufacturer: row?.manufacturer || '',
    category: row?.category || '',
    isSpecial: row?.isSpecial ?? 0
  })
  drugDialogVisible.value = true
}

async function submitDrug() {
  await drugFormRef.value.validate()
  if (drugForm.id) {
    await updateDrug(drugForm.id, drugForm)
    ElMessage.success('修改成功')
  } else {
    await createDrug(drugForm)
    ElMessage.success('新增成功，已自动建立库存行（库存 0，请入库）')
  }
  drugDialogVisible.value = false
  getList()
  loadWarnCount()
}

async function doDeleteDrug(row) {
  await ElMessageBox.confirm(`确认删除药品「${row.drugName}」？`, '提示', { type: 'warning' })
  await deleteDrug(row.id)
  ElMessage.success('删除成功')
  getList()
}

// ---------- 入库/出库 ----------
const stockDialogVisible = ref(false)
const stockForm = reactive({
  drugId: null,
  drugName: '',
  type: 1,
  quantity: 1,
  batchNo: '',
  produceDate: null,
  expireDate: null,
  remark: ''
})

function openStockDialog(row, type) {
  Object.assign(stockForm, {
    drugId: row.id,
    drugName: `${row.drugName}（${row.spec}，当前库存 ${row.quantity}）`,
    type,
    quantity: 1,
    batchNo: '',
    produceDate: null,
    expireDate: null,
    remark: ''
  })
  stockDialogVisible.value = true
}

async function submitStock() {
  if (stockForm.type === 1) {
    await stockIn(stockForm)
    ElMessage.success('入库成功')
  } else {
    await stockOut(stockForm)
    ElMessage.success('出库成功')
  }
  stockDialogVisible.value = false
  getList()
  loadWarnCount()
}

// ---------- 门诊发药 ----------
const dispenseStatus = ref(2)
const dispenseList = ref([])
const dispenseLoading = ref(false)

async function loadDispenseList() {
  dispenseLoading.value = true
  try {
    const res = await getDispensePrescriptions([dispenseStatus.value])
    dispenseList.value = res.data
  } finally {
    dispenseLoading.value = false
  }
}

async function doDispense(row) {
  await ElMessageBox.confirm(`确认发药：${row.prescNo}（￥${row.totalAmount}）？`, '提示', { type: 'warning' })
  await dispense(row.prescNo)
  ElMessage.success('发药成功，库存已扣减')
  loadDispenseList()
  getList()
  loadWarnCount()
}

async function doReturn(row) {
  await ElMessageBox.confirm(`确认退药：${row.prescNo}？库存将回补。`, '提示', { type: 'warning' })
  await returnDrug(row.prescNo)
  ElMessage.success('退药成功，库存已回补')
  loadDispenseList()
  getList()
}

// ---------- 预警 ----------
const warnLoading = ref(false)
const warnList = ref([])

async function loadWarnCount() {
  const res = await getWarnList()
  warnList.value = res.data
  warnCount.value = res.data.length
}

async function loadWarn() {
  warnLoading.value = true
  try {
    await loadWarnCount()
  } finally {
    warnLoading.value = false
  }
}

function onTabChange(tab) {
  if (tab === 'dispense') loadDispenseList()
  if (tab === 'warn') loadWarn()
  if (tab === 'purchase') loadPurchase()
}

// ---------- 采购 ----------
const purchaseVisible = ref(false)
const purchaseLoading = ref(false)
const purchaseList = ref([])
const purchaseStatus = ref(0)
const suppliers = ref([])
const drugAll = ref([])
const purchaseForm = reactive({ supplierId: null, drugId: null, quantity: 100, unitPrice: 10 })

async function loadPurchase() {
  purchaseLoading.value = true
  try {
    const res = await getPurchasePage({ pageNum: 1, pageSize: 50, status: purchaseStatus.value })
    purchaseList.value = res.data.list
  } finally {
    purchaseLoading.value = false
  }
}

function openPurchase() {
  Object.assign(purchaseForm, { supplierId: null, drugId: null, quantity: 100, unitPrice: 10 })
  if (!suppliers.value.length) getSuppliers().then((res) => (suppliers.value = res.data))
  if (!drugAll.value.length) getDrugPage({ pageNum: 1, pageSize: 100 }).then((res) => (drugAll.value = res.data.list))
  purchaseVisible.value = true
}

async function submitPurchase() {
  if (!purchaseForm.supplierId || !purchaseForm.drugId) {
    ElMessage.warning('请选择供应商与药品')
    return
  }
  await createPurchase(purchaseForm)
  ElMessage.success('采购单已创建')
  purchaseVisible.value = false
  loadPurchase()
}

async function doReceive(row) {
  await receivePurchase(row.id)
  ElMessage.success('入库成功：' + row.drugName + ' x' + row.quantity)
  loadPurchase()
  getList()
}

async function doCancelPurchase(row) {
  await cancelPurchase(row.id)
  ElMessage.success('已取消')
  loadPurchase()
}

onMounted(() => {
  getList()
  loadWarnCount()
  getSuppliers().then((res) => (suppliers.value = res.data))
  getDrugPage({ pageNum: 1, pageSize: 100 }).then((res) => (drugAll.value = res.data.list))
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.warn-summary {
  color: #e6a23c;
  font-size: 13px;
}

.low-qty {
  color: #e6a23c;
  font-weight: 700;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
