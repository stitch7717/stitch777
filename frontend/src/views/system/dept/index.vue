<template>
  <el-card shadow="never">
    <template #header>
      <span>科室树（科室即病区，住院病区复用科室ID）</span>
    </template>
    <el-table
      v-loading="loading"
      :data="tree"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column label="科室名称" prop="menuName" min-width="200" />
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="排序" prop="sort" width="80" align="center" />
      <el-table-column label="操作" width="140" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-row class="mt8">
      <el-button type="primary" icon="Plus" @click="openEdit()">新增科室</el-button>
    </el-row>
    <p class="tip">删除保护：存在子科室/人员/床位时不可删除。</p>

    <el-dialog :title="form.id ? '编辑科室' : '新增科室'" v-model="visible" width="420px">
      <el-form label-width="80px">
        <el-form-item label="名称" required><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.deptCode" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.deptType" style="width: 100%">
            <el-option label="门诊" :value="1" /><el-option label="住院" :value="2" />
            <el-option label="医技" :value="3" /><el-option label="行政后勤" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取 消</el-button>
        <el-button type="primary" @click="submit">确 定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptTree, saveDept, updateDept, deleteDept } from '@/api/system'

const loading = ref(false)
const tree = ref([])
const visible = ref(false)
const form = reactive({ id: null, deptName: '', deptCode: '', deptType: 1, sort: 0 })

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除科室「${row.menuName}」？`, '提示', { type: 'warning' })
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  reload()
}

function openEdit(row) {
  Object.assign(form, {
    id: row?.id || null, deptName: row?.menuName || '',
    deptCode: '', deptType: 1, sort: row?.sort ?? 0
  })
  visible.value = true
}

async function submit() {
  if (!form.deptName) {
    ElMessage.warning('请输入名称')
    return
  }
  if (form.id) {
    await updateDept(form.id, form)
    ElMessage.success('修改成功')
  } else {
    await saveDept(form)
    ElMessage.success('新增成功')
  }
  visible.value = false
  reload()
}

async function reload() {
  loading.value = true
  try {
    const res = await getDeptTree()
    tree.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.mt8 { margin-top: 8px; }

.tip {
  margin-top: 12px;
  color: #a8abb2;
  font-size: 12px;
}
</style>
