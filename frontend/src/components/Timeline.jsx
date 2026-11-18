import { formatQuantity, timelineColumns } from '../freshness.js'

/** Frise « jour par jour » : l'élément central de l'interface. */
export default function Timeline({ items }) {
  const columns = timelineColumns(items)
  return (
    <div className="timeline" role="list" aria-label="Aliments par date de péremption" tabIndex={0}>
      {columns.map((col) => (
        <div key={col.key} className={`day day-${col.key}`} role="listitem">
          <h3 className="day-title">{col.title}</h3>
          <ul className="tags">
            {col.items.map((item) => (
              <li key={item.id} className={`tag tag-${item.freshness.toLowerCase()}`}>
                <span className="tag-name">{item.name}</span>
                <span className="tag-qty">{formatQuantity(item.quantity, item.unit)}</span>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  )
}
