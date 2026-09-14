export function ConnectionStatus({ connection, onRetry }) {
  if (connection.status === 'loading') {
    return (
      <div className="connection-state connection-state--loading" role="status">
        <span className="spinner-border spinner-border-sm" aria-hidden="true" />
        Đang kiểm tra kết nối…
      </div>
    )
  }

  if (connection.status === 'error') {
    return (
      <div className="connection-state connection-state--error" role="alert">
        <div>
          <strong>Chưa kết nối được máy chủ.</strong>
          <span>{connection.message}</span>
        </div>
        <button className="btn btn-sm btn-outline-danger" type="button" onClick={onRetry}>
          Thử lại
        </button>
      </div>
    )
  }

  return (
    <div className="connection-state connection-state--connected" role="status">
      <span className="connection-dot" aria-hidden="true" />
      <div>
        <strong>Hệ thống đã sẵn sàng.</strong>
        <span>{connection.message}</span>
      </div>
    </div>
  )
}
