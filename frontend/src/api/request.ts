import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '../types/api'

const instance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Response interceptor
instance.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse
    // If the custom code is not 200, it is judged as an error.
    if (res.code !== 200) {
      ElMessage.error(res.message || 'Error')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res as any
  },
  (error) => {
    console.error('err' + error) // for debug
    ElMessage.error({
      message: error.message || 'Request Error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default instance
