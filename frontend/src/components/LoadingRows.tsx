export function LoadingRows({ cols = 5 }: { cols?: number }) {
  return (
    <>
      {Array.from({ length: 5 }).map((_, i) => (
        <tr key={i} className="skeleton-row" aria-hidden="true">
          {Array.from({ length: cols }).map((__, j) => (
            <td key={j}><span className="skeleton" /></td>
          ))}
        </tr>
      ))}
    </>
  )
}
