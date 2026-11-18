import { CATEGORIES, dayLabel, formatQuantity } from '../freshness.js'

export default function Inventory({ items, onRemove }) {
  if (!items) return null
  if (items.length === 0) return <p className="muted">Le frigo est vide. Ajoutez vos courses ci-dessus.</p>
  return (
    <ul className="inventory">
      {items.map((item) => (
        <li key={item.id} className={`row row-${item.freshness.toLowerCase()}`}>
          <span className="dot" aria-hidden="true" />
          <div className="row-main">
            <span className="row-name">{item.name}</span>
            <span className="row-meta">{formatQuantity(item.quantity, item.unit)}, {CATEGORIES[item.category]}, {dayLabel(item.daysLeft)}</span>
          </div>
          {item.daysLeft < 0
            ? <button className="ghost" onClick={() => onRemove(item, 'jeté')}>Jeter</button>
            : <button className="ghost" onClick={() => onRemove(item, 'consommé')}>Consommé</button>}
        </li>
      ))}
    </ul>
  )
}
