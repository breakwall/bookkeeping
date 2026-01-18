import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
}

export interface AuthResponse {
  id: number
  username: string
  token: string
}

export interface UserInfo {
  id: number
  username: string
  email?: string
}

export const authApi = {
  // 注册
  register(data: RegisterRequest) {
    return request.post<AuthResponse>('/auth/register', data)
  },

  // 登录
  login(data: LoginRequest) {
    return request.post<AuthResponse>('/auth/login', data)
  },

  // 获取当前用户信息
  getCurrentUser() {
    return request.get<UserInfo>('/auth/me')
  },

  // 登出
  logout() {
    return request.post('/auth/logout')
  }
}
