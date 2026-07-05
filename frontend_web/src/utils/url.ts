export function normalizeImageUrl(url: string | null | undefined): string {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      const u = new URL(url)
      return u.pathname + u.search
    } catch {}
  }
  return url
}
