import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    // 登录接口返回原始结构 {code, msg, data}
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.error(res.msg || '未登录或登录已过期')
      router.push('/login')
      return Promise.reject(new Error(res.msg))
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const res = error.response?.data
    if (error.response?.status === 401 || res?.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.error(res?.msg || '未登录或登录已过期')
      router.push('/login')
      return Promise.reject(error)
    }
    if (error.response?.status === 409 || res?.code === 409) {
      ElMessage.error(res?.msg || '数据已变更，请刷新后重试')
      return Promise.reject(error)
    }
    ElMessage.error(res?.msg || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
