/**
 * 认证状态存储:token 与用户信息的 localStorage 读写
 *
 * 唯一的凭证存取入口,request.ts 的拦截器与登录页都通过这里访问,
 * 避免存储 key 散落各处。
 */

const TOKEN_KEY = 'ig_token';
const USER_KEY = 'ig_user';

export interface AuthUser {
  username: string;
  role: string;
}

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function getAuthUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

export function setAuth(token: string, user: AuthUser): void {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function isLoggedIn(): boolean {
  return getToken() !== null;
}

/** 退出登录:清凭证并整页跳转(重置所有组件状态) */
export function logout(): void {
  clearAuth();
  window.location.href = '/login';
}
