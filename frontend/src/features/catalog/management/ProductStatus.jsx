export function ProductStatus({ status }) {
  return <span className={`management-status ${status === 'ACTIVE' ? 'is-active' : ''}`}>{status === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán'}</span>
}
