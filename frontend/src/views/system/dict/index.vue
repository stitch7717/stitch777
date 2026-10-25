<template>
  <el-card shadow="never">
    <el-row :gutter="16">
      <!-- 左：字典类型 -->
      <el-col :span="6">
        <div class="panel">
          <div class="panel-head">字典类型</div>
          <div
            v-for="t in types"
            :key="t.id"
            class="type-item"
            :class="{ active: currentType === t.dictType }"
            @click="selectType(t.dictType)"
          >
            <span>{{ t.dictName }}</span>
            <span class="type-code">{{ t.dictType }}</span>
          </div>
        </div>
      </el-col>

      <!-- 右：字典数据 -->
      <el-col :span="18">
        <el-row class="mb8">
          <el-button type="primary" icon="Plus" :disabled="!currentType" @click="openAdd">
            新增{{ currentTypeName }}标签
          </el-button>
          <span class="tip">标签三色分级：red 高风险 / yellow 关注 / green 常规，空为中性</span>
        </el-row>
        <el-table v-loading="loading" :data="dataList" border stripe>
          <el-table-column label="标签" prop="dictLabel" min-width="120" />
          <el-table-column label="键值" prop="dictValue" min-width="120" />
          <el-table-column label="颜色" width="110" align="center">
            <template #default="{ row }">
              <span class="color-dot" :style="dotStyle(row.color)" />
              {{ colorText(row.color) }}
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="sort" width="70" align="center" />
          <el-table-column label="操作" width="170" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openColor(row)">改色</el-button>
              <el-button link type="danger" @click="doDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <!-- 新增标签 -->
    <el-dialog title="新增标签" v-model="addVisible" width="420px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="标签" required>
          <el-input v-model="addForm.dictLabel" placeholder="如：传染病" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-radio-group v-model="addForm.color">
            <el-radio value="">中性</el-radio>
            <el-radio value="red">红(高风险)</el-radio>
            <el-radio value="yellow">黄(关注)</el-radio>
            <el-radio value="green">绿(常规)</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitAdd">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 改色 -->
    <el-dialog title="修改标签颜色" v-model="colorVisible" width="420px">
      <p style="margin-bottom: 12px">标签：<b>{{ colorRow?.dictLabel }}</b></p>
      <el-radio-group v-model="colorValue">
        <el-radio value="red">红(高风险)</el-radio>
        <el-radio value="yellow">黄(关注)</el-radio>
        <el-radio value="green">绿(常规)</el-radio>
      </el-radio-group>
      <p class="tip" style="margin-top: 10px">按文档口径：编辑仅支持改色（不支持恢复中性，需中性可删除后重建）</p>
      <template #footer>
        <el-button @click="colorVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitColor">确 定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictTypes, getDictData, addDictData, updateDictColor, deleteDictData } from '@/api/system'

const types = ref([])
const currentType = ref(null)
const dataList = ref([])
const loading = ref(false)

const currentTypeName = computed(() =>
  types.value.find((t) => t.dictType === currentType.value)?.dictName || ''
)

async function loadTypes() {
  const res = await getDictTypes()
  types.value = res.data
  if (types.value.length && !currentType.value) {
    selectType(types.value[0].dictType)
  }
}

async function selectType(dictType) {
  currentType.value = dictType
  loading.value = true
  try {
    const res = await getDictData(dictType)
    dataList.value = res.data
  } finally {
    loading.value = false
  }
}

// ---------- 新增 ----------
const addVisible = ref(false)
const addForm = reactive({ dictLabel: '', color: '' })

function openAdd() {
  addForm.dictLabel = ''
  addForm.color = ''
  addVisible.value = true
}

async function submitAdd() {
  if (!addForm.dictLabel) {
    ElMessage.warning('请输入标签')
    return
  }
  await addDictData({
    dictType: currentType.value,
    dictLabel: addForm.dictLabel,
    dictValue: addForm.dictLabel,
    color: addForm.color || null
  })
  ElMessage.success('新增成功')
  addVisible.value = false
  selectType(currentType.value)
}

// ---------- 改色 ----------
const colorVisible = ref(false)
const colorRow = ref(null)
const colorValue = ref('red')

function openColor(row) {
  colorRow.value = row
  colorValue.value = row.color || 'red'
  colorVisible.value = true
}

async function submitColor() {
  await updateDictColor(colorRow.value.id, colorValue.value)
  ElMessage.success('已更新')
  colorVisible.value = false
  selectType(currentType.value)
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除标签「${row.dictLabel}」？删除后可重建同名。`, '提示', {
    type: 'warning'
  })
  await deleteDictData(row.id)
  ElMessage.success('删除成功')
  selectType(currentType.value)
}

function colorText(c) {
  return { red: '红', yellow: '黄', green: '绿' }[c] || '中性'
}

function dotStyle(c) {
  const map = { red: '#f56c6c', yellow: '#e6a23c', green: '#67c23a' }
  return {
    display: 'inline-block',
    width: '12px',
    height: '12px',
    borderRadius: '50%',
    background: map[c] || '#dcdfe6',
    marginRight: '6px',
    verticalAlign: 'middle'
  }
}

onMounted(loadTypes)
</script>

<style scoped>
.panel {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.panel-head {
  padding: 10px 14px;
  background: #f5f7fa;
  font-weight: 600;
  font-size: 14px;
}

.type-item {
  padding: 10px 14px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  border-bottom: 1px solid #f0f2f5;
}

.type-item:hover {
  background: #f5f7fa;
}

.type-item.active {
  background: #ecf5ff;
  color: #1f6feb;
  font-weight: 600;
}

.type-code {
  color: #a8abb2;
  font-size: 12px;
}

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

.color-dot {
  display: inline-block;
}
</style>
