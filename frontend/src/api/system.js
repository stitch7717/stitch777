import request from '@/utils/request'

export const login = (data) => request.post('/api/system/user/login', data)
export const logout = () => request.post('/api/system/user/logout')
export const getUserPage = (params) => request.get('/api/system/user/page', { params })
export const getCurrentMenus = () => request.get('/api/system/menu/current')
export const getDeptTree = () => request.get('/api/system/dept/tree')
export const getRoleList = () => request.get('/api/system/role/list')
export const getDictData = (dictType) =>
  request.get('/api/system/dict/data', { params: { dictType } })
export const getDictTypes = () => request.get('/api/system/dict/type/list')
export const addDictData = (data) => request.post('/api/system/dict/data', data)
export const updateDictColor = (id, color) =>
  request.put(`/api/system/dict/data/${id}`, { color })
export const deleteDictData = (id) => request.delete(`/api/system/dict/data/${id}`)
export const getRoleDetail = (id) => request.get(`/api/system/role/${id}`)
export const createRole = (data) => request.post('/api/system/role', data)
export const updateRole = (id, data) => request.put(`/api/system/role/${id}`, data)
export const assignRolePermission = (id, menuIds) =>
  request.put(`/api/system/role/${id}/permission`, { menuIds })
export const deleteRole = (id) => request.delete(`/api/system/role/${id}`)
export const getFullMenuTree = () => request.get('/api/system/menu/tree')

// ---- 患者管理 ----
export const getPatientPage = (params) => request.get('/api/patient/page', { params })
export const getPatientDetail = (id) => request.get(`/api/patient/${id}`)
export const createPatient = (data) => request.post('/api/patient', data)
export const updatePatient = (id, data) => request.put(`/api/patient/${id}`, data)
export const deletePatient = (id) => request.delete(`/api/patient/${id}`)
export const lossPatientCard = (id) => request.put(`/api/patient/card/${id}/loss`)
export const reissuePatientCard = (patientId) =>
  request.post('/api/patient/card/reissue', { patientId })

// ---- 挂号与预约 ----
export const getScheduleList = (params) => request.get('/api/registration/schedule', { params })
export const createSchedule = (data) => request.post('/api/registration/schedule', data)
export const stopSchedule = (id) => request.put(`/api/registration/schedule/${id}/stop`)
export const register = (data) => request.post('/api/registration', data)
export const cancelRegistration = (id) => request.put(`/api/registration/${id}/cancel`)
export const getQueue = (deptId) => request.get(`/api/registration/queue/${deptId}`)
export const callNumber = (id) => request.post(`/api/registration/call/${id}`)
export const getRegDepts = () => request.get('/api/registration/depts')
export const getRegDoctors = (deptId) =>
  request.get('/api/registration/doctors', { params: { deptId } })

// ---- 门诊管理 ----
export const getVisitList = (params) => request.get('/api/clinic/visit/page', { params })
export const receiveVisit = (id) => request.post(`/api/clinic/visit/${id}/receive`)
export const saveRecord = (data) => request.post('/api/clinic/visit/record', data)
export const getRecordByVisitNo = (visitNo) =>
  request.get('/api/clinic/visit/record', { params: { visitNo } })
export const createPrescription = (data) => request.post('/api/clinic/prescription', data)
export const getPrescriptionPage = (params) =>
  request.get('/api/clinic/prescription/page', { params })
export const auditPrescription = (id) => request.put(`/api/clinic/prescription/${id}/audit`)
export const voidPrescription = (id) => request.delete(`/api/clinic/prescription/${id}`)
export const getClinicDrugs = () => request.get('/api/clinic/drugs')

// ---- 药品与药房 ----
export const getDrugPage = (params) => request.get('/api/drug/page', { params })
export const createDrug = (data) => request.post('/api/drug', data)
export const updateDrug = (id, data) => request.put(`/api/drug/${id}`, data)
export const deleteDrug = (id) => request.delete(`/api/drug/${id}`)
export const stockIn = (data) => request.post('/api/drug/stock/in', data)
export const stockOut = (data) => request.post('/api/drug/stock/out', data)
export const getWarnList = () => request.get('/api/drug/stock/warn')
export const dispense = (prescNo) => request.post('/api/pharmacy/dispense', { prescNo })
export const returnDrug = (prescNo) => request.post('/api/pharmacy/return', { prescNo })
export const getDispensePrescriptions = (statuses) =>
  request.get('/api/pharmacy/prescription/list', { params: { statuses } })

// ---- 收费财务 ----
export const getPendingCharges = () => request.get('/api/charge/pending')
export const getPendingDetail = (visitId) => request.get(`/api/charge/pending/${visitId}`)
export const doCharge = (data) => request.post('/api/charge', data)
export const refundCharge = (id) => request.post(`/api/charge/${id}/refund`)
export const getChargePage = (params) => request.get('/api/charge/page', { params })
export const getDailyReport = (date) =>
  request.get('/api/finance/daily', { params: { date } })

// ---- 住院管理 ----
export const getBeds = (params) => request.get('/api/inpatient/bed/list', { params })
export const getFreeBeds = (wardId) => request.get('/api/inpatient/beds/free', { params: { wardId } })
export const admitPatient = (data) => request.post('/api/inpatient/admission', data)
export const dischargeAdmission = (id) => request.put(`/api/inpatient/admission/${id}/discharge`)
export const transferAdmission = (id, data) => request.post(`/api/inpatient/admission/${id}/transfer`, data)
export const getAdmissionDetail = (id) => request.get(`/api/inpatient/admission/${id}`)
export const addOrder = (data) => request.post('/api/inpatient/order', data)
export const executeOrder = (id) => request.put(`/api/inpatient/order/${id}/execute`)
export const stopOrder = (id) => request.put(`/api/inpatient/order/${id}/stop`)
export const addNursing = (data) => request.post('/api/inpatient/nursing', data)
export const getInpatientDepts = () => request.get('/api/inpatient/depts')
export const getInpatientDoctors = (deptId) => request.get('/api/inpatient/doctors', { params: { deptId } })
export const getInpatientNurses = (deptId) => request.get('/api/inpatient/nurses', { params: { deptId } })

// ---- 检查检验 ----
export const createExamApply = (data) => request.post('/api/exam/apply', data)
export const getExamPage = (params) => request.get('/api/exam/apply/page', { params })
export const examSample = (applyId) => request.post('/api/exam/sample', { applyId })
export const saveExamReport = (data) => request.post('/api/exam/report', data)
export const auditExamReport = (id) => request.put(`/api/exam/report/${id}/audit`)
export const publishExamReport = (id) => request.put(`/api/exam/report/${id}/publish`)
export const getExamReport = (id) => request.get(`/api/exam/report/${id}`)
export const handleCritical = (reportId, remark) =>
  request.post('/api/exam/critical/handle', { reportId, remark })

// ---- 公共卫生 ----
export const getHealthRecords = (params) => request.get('/api/health/record/page', { params })
export const createHealthRecord = (data) => request.post('/api/health/record', data)
export const updateHealthRecord = (id, data) => request.put(`/api/health/record/${id}`, data)
export const getVaccinations = (params) => request.get('/api/health/vaccination/page', { params })
export const addVaccination = (data) => request.post('/api/health/vaccination', data)
export const reportInfectious = (data) => request.post('/api/health/infectious/report', data)
export const getInfectiousPage = (params) => request.get('/api/health/infectious/page', { params })
export const getCheckupPage = (params) => request.get('/api/health/checkup/page', { params })
export const addCheckup = (data) => request.post('/api/health/checkup', data)
export const getVaccinationRemind = () => request.get('/api/health/vaccination/remind')

// ---- 数据安全补充 ----
export const getOperationModules = () => request.get('/api/audit/operation/modules')

// ---- 统计报表 ----
export const getOutpatientReport = (params) => request.get('/api/report/outpatient', { params })
export const getInpatientReport = (params) => request.get('/api/report/inpatient', { params })
export const getIncomeReport = (params) => request.get('/api/report/income', { params })
export const getDrugReport = (params) => request.get('/api/report/drug', { params })
export const getPatientReport = () => request.get('/api/report/patient')
export const getOverview = () => request.get('/api/report/overview')
export const getReportDepts = () => request.get('/api/report/depts')

// ---- 消息通知 ----
export const getMessagePage = (params) => request.get('/api/message/page', { params })
export const readMessage = (id) => request.put(`/api/message/${id}/read`)
export const readAllMessages = () => request.put('/api/message/read-all')
export const getUnreadCount = () => request.get('/api/message/unread-count')
export const publishNotice = (data) => request.post('/api/notice', data)
export const getNoticePage = (params) => request.get('/api/notice/page', { params })
export const deleteNotice = (id) => request.delete(`/api/notice/${id}`)
export const getWarningList = () => request.get('/api/warning/list')

// ---- 数据安全 ----
export const getOperationLogs = (params) => request.get('/api/audit/operation/page', { params })
export const getLoginLogs = (params) => request.get('/api/audit/login/page', { params })
export const getBackupPage = (params) => request.get('/api/backup/page', { params })
export const createBackup = () => request.post('/api/backup')
export const restoreBackup = (id) => request.post(`/api/backup/${id}/restore`, { confirm: true })
export const deleteBackup = (id) => request.delete(`/api/backup/${id}`)
export const getSecurityConfig = () => request.get('/api/security/config')
export const updateSecurityConfig = (data) => request.put('/api/security/config', data)

// ---- 功能补齐 ----
export const createUser = (data) => request.post('/api/system/user', data)
export const updateUser = (id, data) => request.put(`/api/system/user/${id}`, data)
export const deleteUser = (id) => request.delete(`/api/system/user/${id}`)
export const changeUserStatus = (id, status) => request.put(`/api/system/user/${id}/status`, { status })
export const resetUserPassword = (id, password) => request.put(`/api/system/user/${id}/password`, { password })
export const saveDept = (data) => request.post('/api/system/dept', data)
export const updateDept = (id, data) => request.put(`/api/system/dept/${id}`, data)
export const deleteDept = (id) => request.delete(`/api/system/dept/${id}`)
export const createFollowUp = (data) => request.post('/api/patient/followup', data)
export const getFollowUpPage = (params) => request.get('/api/patient/followup/page', { params })
export const completeFollowUp = (id, result) => request.put(`/api/patient/followup/${id}/complete`, { result })
export const getPatientRecords = (id) => request.get(`/api/patient/${id}/records`)
export const printInvoice = (chargeId, invoiceType) => request.post('/api/invoice/print', { chargeId, invoiceType })
export const getInvoicePage = (params) => request.get('/api/invoice/page', { params })
export const voidInvoice = (id) => request.put(`/api/invoice/${id}/void`)
export const settlePreview = (id) => request.get(`/api/inpatient/admission/${id}/settle/preview`)
export const settleAdmission = (id) => request.post(`/api/inpatient/admission/${id}/settle`)
export const getSuppliers = () => request.get('/api/pharmacy/supplier/list')
export const addSupplier = (data) => request.post('/api/pharmacy/supplier', data)
export const getPurchasePage = (params) => request.get('/api/pharmacy/purchase/page', { params })
export const createPurchase = (data) => request.post('/api/pharmacy/purchase', data)
export const receivePurchase = (id) => request.put(`/api/pharmacy/purchase/${id}/receive`)
export const cancelPurchase = (id) => request.put(`/api/pharmacy/purchase/${id}/cancel`)
export const getMaternalPage = (params) => request.get('/api/health/maternal/page', { params })
export const addMaternal = (data) => request.post('/api/health/maternal', data)
export const getChildCarePage = (params) => request.get('/api/health/childcare/page', { params })
export const addChildCare = (data) => request.post('/api/health/childcare', data)
export const sendSms = (data) => request.post('/api/sms/send', data)
export const getSmsPage = (params) => request.get('/api/sms/page', { params })
export const getCustomDatasets = () => request.get('/api/report/custom/datasets')
export const executeCustom = (config) => request.post('/api/report/custom/execute', config)
export const saveCustomReport = (data) => request.post('/api/report/custom', data)
export const getCustomPage = (params) => request.get('/api/report/custom/page', { params })
export const getCustomDetail = (id) => request.get(`/api/report/custom/${id}`)
export const deleteCustomReport = (id) => request.delete(`/api/report/custom/${id}`)
export const saveMenu = (data) => request.post('/api/system/menu', data)
export const updateMenu = (id, data) => request.put(`/api/system/menu/${id}`, data)
export const deleteMenu = (id) => request.delete(`/api/system/menu/${id}`)

export const registerOrGet = (data) => request.post('/api/patient/register-or-get', data)
