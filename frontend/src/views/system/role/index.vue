<template>
  <el-card shadow="never">
    <el-row class="mb8">
      <el-button type="primary" icon="Plus" @click="openEdit()">新增角色</el-button>
    </el-row>

    <el-table v-loading="loading" :data="roles" border stripe>
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="角色名称" prop="roleName" min-width="110" />
      <el-table-column label="角色编码" prop="roleCode" min-width="110" />
      <el-table-column label="备注" prop="remark" min-width="180" />
      <el-table-column label="用户数" prop="userCount" width="80" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openPerm(row)">分配权限</el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑角色 -->
    <el-dialog :title="form.id ? '编辑角色' : '新增角色'" v-model="editVisible" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" required>
          <el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="如 doctor（创建后不可改）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitEdit">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog :title="`分配权限：${permRole?.roleName || ''}`" v-model="permVisible" width="460px">
      <el-tree
        ref="permTreeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'menuName', children: 'children' }"
        default-expand-all
        :default-checked-keys="checkedKeys"
      />
      <template #footer>
        <el-button @click="permVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitPerm">保 存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRoleList,
  getRoleDetail,
  createRole,
  updateRole,
  assignRolePermission,
  deleteRole,
  getFullMenuTree
} from '@/api/system'

const loading = ref(false)
const roles = ref([])

async function getList() {
  loading.value = true
  try {
    const res = await getRoleList()
    roles.value = res.data
  } finally {
    loading.value = false
  }
}

// ---------- 编辑 ----------
const editVisible = ref(false)
const form = reactive({ id: null, roleName: '', roleCode: '', remark: '', status: 1 })

function openEdit(row) {
  Object.assign(form, {
    id: row?.id || null,
    roleName: row?.roleName || '',
    roleCode: row?.roleCode || '',
    remark: row?.remark || '',
    status: row?.status ?? 1
  })
  editVisible.value = true
}

async function submitEdit() {
  if (!form.roleName) {
    ElMessage.warning('请输入角色名称')
    return
  }
  if (form.id) {
    await updateRole(form.id, form)
    ElMessage.success('修改成功')
  } else {
    if (!form.roleCode) {
      ElMessage.warning('请输入角色编码')
      return
    }
    await createRole(form)
    ElMessage.success('新增成功')
  }
  editVisible.value = false
  getList()
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  getList()
}

// ---------- 分配权限 ----------
const permVisible = ref(false)
const permRole = ref(null)
const menuTree = ref([])
const checkedKeys = ref([])
const permTreeRef = ref()

async function openPerm(row) {
  permRole.value = row
  if (!menuTree.value.length) {
    const res = await getFullMenuTree()
    menuTree.value = res.data
  }
  const detail = await getRoleDetail(row.id)
  // 只勾选叶子，父节点由 el-tree 半选联动
  checkedKeys.value = (detail.data.menuIds || []).filter((id) => isLeaf(id, menuTree.value))
  permVisible.value = true
}

function isLeaf(id, nodes) {
  for (const n of nodes) {
    if (n.id === id) return !n.children || n.children.length === 0
    const found = isLeaf(id, n.children || [])
    if (found !== null && found !== undefined) return found
  }
  return null
}

async function submitPerm() {
  // getCheckedKeys + getHalfCheckedKeys：勾选与半选父节点一起提交
  const keys = [
    ...permTreeRef.value.getCheckedKeys(),
    ...permTreeRef.value.getHalfCheckedKeys()
  ]
  await assignRolePermission(permRole.value.id, keys)
  ElMessage.success('权限已保存')
  permVisible.value = false
  getList()
}

onMounted(getList)
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
