import { useState } from 'react'
import { CATEGORIES, isoDateIn } from '../freshness.js'

const UNITS = ['pcs', 'g', 'kg', 'L', 'cl']
const empty = () => ({ name: '', quantity: '1', unit: 'pcs', category: 'VEGETABLE', expiresOn: isoDateIn(3) })

export default function AddItemForm({ onAdd }) {
  const [form, setForm] = useState(empty)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      await onAdd({ ...form, name: form.name.trim(), quantity: Number(form.quantity) })
      setForm(empty())
      setError(null)
    } catch (err) {
      setError(`L'aliment n'a pas été ajouté : ${err.message}`)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="add-form" onSubmit={submit}>
      <label className="field field-wide">
        Nom
        <input required maxLength={80} value={form.name} onChange={set('name')} placeholder="Courgette" />
      </label>
      <label className="field">
        Quantité
        <input required type="number" min="0.01" step="0.01" value={form.quantity} onChange={set('quantity')} />
      </label>
      <label className="field">
        Unité
        <select value={form.unit} onChange={set('unit')}>
          {UNITS.map((u) => <option key={u}>{u}</option>)}
        </select>
      </label>
      <label className="field">
        Rayon
        <select value={form.category} onChange={set('category')}>
          {Object.entries(CATEGORIES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </label>
      <label className="field">
        À consommer avant
        <input required type="date" value={form.expiresOn} onChange={set('expiresOn')} />
      </label>
      {error && <p className="alert field-wide" role="alert">{error}</p>}
      <button className="primary field-wide" disabled={busy}>{busy ? 'Ajout…' : 'Ajouter au frigo'}</button>
    </form>
  )
}
