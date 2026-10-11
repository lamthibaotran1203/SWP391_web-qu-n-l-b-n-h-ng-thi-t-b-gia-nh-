import { useState } from 'react'

export function ProductThumbnail({ src, large = false, name = '' }) {
  const [failedSource, setFailedSource] = useState(null)
  const className = large ? 'management-large-image' : 'management-thumbnail'
  return src && failedSource !== src
    ? <img className={className} src={src} alt={name} width={large ? 320 : 64} height={large ? 320 : 64}
      loading="lazy" decoding="async" onError={() => setFailedSource(src)} />
    : <span className={`${className} management-thumbnail-empty`}>Chưa có ảnh</span>
}
