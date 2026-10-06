import { useState } from 'react'

export function ProductThumbnail({ src }) {
  const [failedSource, setFailedSource] = useState(null)
  return src && failedSource !== src
    ? <img className="management-thumbnail" src={src} alt="" width={64} height={64}
      loading="lazy" decoding="async" onError={() => setFailedSource(src)} />
    : <span className="management-thumbnail management-thumbnail-empty">Chưa có ảnh</span>
}
