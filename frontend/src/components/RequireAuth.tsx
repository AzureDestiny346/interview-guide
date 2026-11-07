import { Navigate, useLocation } from 'react-router-dom';
import type { ReactElement } from 'react';
import { isLoggedIn } from '../auth/authStorage';

/**
 * 路由守卫:未登录访问业务页面时重定向到 /login,记住来源路径
 *
 * 只做凭证存在性检查;token 过期由 axios 401 拦截兜底引导重新登录。
 */
export default function RequireAuth({ children }: { children: ReactElement }) {
  const location = useLocation();

  if (!isLoggedIn()) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }

  return children;
}
