<template>
  <el-card shadow="never">
    <el-row class="mb8">
      <el-button type="primary" icon="Plus" @click="openEdit()">新增菜单</el-button>
      <span class="tip">菜单类型：1目录 / 2菜单 / 3按钮；删除保护：有子菜单先删子菜单</span>
    </el-row>
    <el-table v-loading="loading" :data="tree" row-key="id" border default-expand-all
      :tree-props="{ children: 'children' }">
      <el-table-column label="名称" prop="menuName" min-width="180" />
      <el-table-column label="ID" prop="id" width="70" align="center" />
      <el-table-column label="路由" prop="path" min-width="120" />
      <el-table-column label="权限标识" prop="perms" min-width="160" />
      <el-table-column label="类型" width="70" align="center">
        <template #default="{ row }">{{ ['', '目录', '菜单', '按钮'][row.menuType] }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="form.id ? '编辑菜单' : '新增菜单'" v-model="visible" width="460px">
      <el-form label-width="90px">
        <el-form-item label="名称" required><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item label="路由"><el-input v-model="form.path" placeholder="如 user / /system" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.perms" placeholder="如 system:user:edit" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.menuType" style="width: 100%">
            <el-option label="目录" :value="1" /><el-option label="菜单" :value="2" />
            <el-option label="按钮" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio><el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { getFullMenuTree, saveMenu, updateMenu, deleteMenu } from '@/api/system'

const loading = ref(false)
const tree = ref([])
const visible = ref(false)
const form = reactive({
  id: null, menuName: '', path: '', perms: '', menuType: 2, sort: 0, status: 1
})

async function load() {
  loading.value = true
  try {
    const res = await getFullMenuTree()
    tree.value = res.data
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, {
    id: row?.id || null, menuName: row?.menuName || '', path: row?.path || '',
    perms: row?.perms || '', menuType: row?.menuType ?? 2,
    sort: row?.sort ?? 0, status: row?.status ?? 1
  })
  visible.value = true
}

async function submit() {
  if (!form.menuName) {
    ElMessage.warning('请输入名称')
    return
  }
  if (form.id) {
    await updateMenu(form.id, form)
    ElMessage.success('修改成功')
  } else {
    await saveMenu(form)
    ElMessage.success('新增成功')
  }
  visible.value = false
  load()
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
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
</style>
