const resolveEnv = (val: string | undefined, fallback: string) =>
  val !== undefined && val !== '' ? val : fallback

export const API_BASE = resolveEnv(import.meta.env.VITE_API_BASE, '')

export const MINIO_BASE = resolveEnv(import.meta.env.VITE_MINIO_BASE, 'http://localhost:9000')

/** 将 HTTP 图片 URL 转为相对路径（走 Vite 代理，避免 HTTPS 页面混合内容拦截） */
export function proxyImageUrl(url: string | null | undefined): string {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      const u = new URL(url)
      return u.pathname + u.search
    } catch {}
  }
  return url
}
