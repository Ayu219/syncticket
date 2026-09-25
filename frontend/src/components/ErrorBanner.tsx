interface ErrorBannerProps {
  message: string
  onRetry?: () => void
  onReload?: () => void
  retryLabel?: string
}

export function ErrorBanner({
  message,
  onRetry,
  onReload,
  retryLabel = 'Retry',
}: ErrorBannerProps) {
  return (
    <div className="error-banner" role="alert">
      <p>{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          {retryLabel}
        </button>
      )}
      {onReload && (
        <button type="button" className="btn btn-secondary" onClick={onReload}>
          Reload
        </button>
      )}
    </div>
  )
}
