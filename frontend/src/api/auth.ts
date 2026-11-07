import request from './request';

/**
 * 认证 API:注册 / 登录
 */

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export type RegisterPayload = LoginPayload;

export function login(payload: LoginPayload): Promise<AuthResponse> {
  return request.post<AuthResponse>('/api/auth/login', payload);
}

export function register(payload: RegisterPayload): Promise<AuthResponse> {
  return request.post<AuthResponse>('/api/auth/register', payload);
}
