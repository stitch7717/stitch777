<template>
  <el-card shadow="never">
    <!-- 查询表单 -->
    <el-form :inline="true" :model="queryParams" @submit.prevent>
      <el-form-item label="关键字">
        <el-input
          v-model="queryParams.keyword"
          placeholder="账号/姓名"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="科室">
        <el-tree-select
          v-model="queryParams.deptId"
          :data="deptTree"
          :props="{ label: 'menuName', value: 'id' }"
          check-strictly
          clearable
          placeholder="全部"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="角色">
        <el-select
          v-model="queryParams.roleId"
          placeholder="全部"
          clearable
          style="width: 140px"
        >
          <el-option
            v-for="role in roleList"
            :key="role.id"
            :label="role.roleName"
            :value="role.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select
          v-model="queryParams.status"
          placeholder="全部"
          clearable
          style="width: 120px"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row class="mb8">
      <el-button type="primary" icon="Plus" @click="openEdit()">新增用户</el-button>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="userList" border stripe>
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="账号" prop="username" min-width="110" />
      <el-table-column label="姓名" prop="realName" min-width="100" />
      <el-table-column label="性别" width="70" align="center">
        <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
      </el-table-column>
      <el-table-column label="手机号" prop="phone" min-width="120" />
      <el-table-column label="科室" prop="deptName" min-width="100" />
      <el-table-column label="角色" prop="roleName" min-width="100" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" min-width="160" />
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="doStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="info" @click="doResetPwd(row)">重置密码</el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
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
    <!-- 新增/编辑用户 -->
    <el-dialog :title="editForm.id ? '编辑用户' : '新增用户'" v-model="editVisible" width="480px">
      <el-form label-width="80px">
        <el-form-item label="账号" required>
          <el-input v-model="editForm.username" :disabled="!!editForm.id" />
        </el-form-item>
        <el-form-item v-if="!editForm.id" label="初始密码" required>
          <el-input v-model="editForm.password" show-password />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="editForm.realName" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
        <el-form-item label="科室">
          <el-tree-select v-model="editForm.deptId" :data="deptTree"
            :props="{ label: 'menuName', value: 'id' }" check-strictly clearable style="width: 100%" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="editForm.roleId" style="width: 100%">
            <el-option v-for="r in roleList" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitEdit">确 定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserPage, getDeptTree, getRoleList, createUser, updateUser,
  deleteUser, changeUserStatus, resetUserPassword
} from '@/api/system'

const editVisible = ref(false)
const editForm = reactive({
  id: null, username: '', password: '', realName: '', gender: 1,
  phone: '', deptId: null, roleId: null
})

function openEdit(row) {
  Object.assign(editForm, {
    id: row?.id || null,
    username: row?.username || '',
    password: '',
    realName: row?.realName || '',
    gender: row?.gender ?? 1,
    phone: row?.phone || '',
    deptId: row?.deptId || null,
    roleId: row?.roleId || null
  })
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.username) {
    ElMessage.warning('请输入账号')
    return
  }
  if (editForm.id) {
    await updateUser(editForm.id, editForm)
    ElMessage.success('修改成功')
  } else {
    if (!editForm.password) {
      ElMessage.warning('请输入初始密码')
      return
    }
    await createUser(editForm)
    ElMessage.success('新增成功')
  }
  editVisible.value = false
  getList()
}

async function doStatus(row) {
  await changeUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  getList()
}

async function doResetPwd(row) {
  await ElMessageBox.confirm(`确认重置「${row.username}」的密码为 123456？`, '提示', { type: 'warning' })
  await resetUserPassword(row.id, null)
  ElMessage.success('密码已重置为 123456')
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  getList()
}
const loading = ref(false)
const userList = ref([])
const total = ref(0)
const deptTree = ref([])
const roleList = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  deptId: null,
  roleId: null,
  status: null
})

async function getList() {
  loading.value = true
  try {
    const res = await getUserPage(queryParams)
    userList.value = res.data.list
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
    deptId: null,
    roleId: null,
    status: null
  })
  getList()
}

onMounted(async () => {
  getList()
  const [deptRes, roleRes] = await Promise.all([getDeptTree(), getRoleList()])
  deptTree.value = deptRes.data
  roleList.value = roleRes.data
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}

.todo-tip {
  margin-left: 12px;
  color: #a8abb2;
  font-size: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
