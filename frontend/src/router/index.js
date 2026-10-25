import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/overview',
    children: [
      {
        path: 'overview',
        name: 'Overview',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据总览' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/dept',
        name: 'SystemDept',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '科室管理' }
      },
      {
        path: 'system/menus',
        name: 'SystemMenus',
        component: () => import('@/views/system/menus/index.vue'),
        meta: { title: '菜单管理' }
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理' }
      },
      {
        path: 'inpatient/beds',
        name: 'InpatientBeds',
        component: () => import('@/views/inpatient/beds/index.vue'),
        meta: { title: '床位一览' }
      },
      {
        path: 'exam/lab',
        name: 'ExamLab',
        component: () => import('@/views/exam/lab/index.vue'),
        meta: { title: '检验工作台' }
      },
      {
        path: 'health/records',
        name: 'HealthRecords',
        component: () => import('@/views/health/records/index.vue'),
        meta: { title: '健康档案' }
      },
      {
        path: 'audit/logs',
        name: 'AuditLogs',
        component: () => import('@/views/audit/logs/index.vue'),
        meta: { title: '审计日志' }
      },
      {
        path: 'report/reports',
        name: 'ReportCenter',
        component: () => import('@/views/report/reports/index.vue'),
        meta: { title: '报表中心' }
      },
      {
        path: 'message/center',
        name: 'MessageCenter',
        component: () => import('@/views/message/center/index.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'patient/archive',
        name: 'PatientArchive',
        component: () => import('@/views/patient/archive/index.vue'),
        meta: { title: '患者档案' }
      },
      {
        path: 'registration/walkin',
        name: 'RegistrationWalkin',
        component: () => import('@/views/registration/walkin/index.vue'),
        meta: { title: '现场挂号' }
      },
      {
        path: 'clinic/console',
        name: 'ClinicConsole',
        component: () => import('@/views/clinic/console/index.vue'),
        meta: { title: '门诊工作台' }
      },
      {
        path: 'pharmacy/stock',
        name: 'PharmacyStock',
        component: () => import('@/views/pharmacy/stock/index.vue'),
        meta: { title: '药品库存' }
      },
      {
        path: 'finance/charge',
        name: 'FinanceCharge',
        component: () => import('@/views/finance/charge/index.vue'),
        meta: { title: '门诊收费' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/overview'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    return { path: '/login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  if (to.path === '/login' && userStore.token) {
    return '/overview'
  }
  document.title = to.meta?.title ? `${to.meta.title} - 医疗后台管理系统` : '医疗后台管理系统'
  return true
})

export default router
