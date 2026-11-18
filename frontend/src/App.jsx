import { useCallback, useEffect, useState } from 'react'
import { api } from './api.js'
import { headline } from './freshness.js'
import Timeline from './components/Timeline.jsx'
import Suggestions from './components/Suggestions.jsx'
import AddItemForm from './components/AddItemForm.jsx'
import Inventory from './components/Inventory.jsx'

export default function App() {
  const [items, setItems] = useState(null)
  const [suggestions, setSuggestions] = useState(null)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)

  const refresh = useCallback(async () => {
    try {
      const [list, sugg] = await Promise.all([api.items(), api.suggestions(6)])
      setItems(list)
      setSuggestions(sugg)
      setError(null)
    } catch (e) {
      setError(`Impossible de lire le frigo : ${e.message}. Vérifiez que la gateway est joignable puis rechargez.`)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])

  useEffect(() => {
    if (!notice) return undefined
    const t = setTimeout(() => setNotice(null), 3500)
    return () => clearTimeout(t)
  }, [notice])

  const add = async (item) => {
    await api.addItem(item)
    setNotice(`${item.name} ajouté au frigo`)
    await refresh()
  }

  const remove = async (item, verb) => {
    await api.removeItem(item.id)
    setNotice(`${item.name} ${verb}`)
    await refresh()
  }

  return (
    <div className="page">
      <header className="masthead">
        <a className="brand" href="/" aria-label="Fridge Rescue, accueil">
          <svg viewBox="0 0 32 32" aria-hidden="true"><rect x="7" y="2" width="18" height="28" rx="4" /><rect className="door" x="7" y="12" width="18" height="2" /></svg>
          Fridge Rescue
        </a>
        <a className="skip" href="#ajouter">Ajouter un aliment</a>
      </header>

      <main>
        <section className="hero" aria-labelledby="titre">
          <h1 id="titre">{items ? headline(items) : 'Ouverture du frigo…'}</h1>
          <p className="lede">Chaque aliment est placé selon le nombre de jours qui lui restent. Les recettes proposées utilisent d'abord ceux de gauche.</p>
          {error && <p className="alert" role="alert">{error}</p>}
          {items && <Timeline items={items} />}
        </section>

        <div className="columns">
          <section aria-labelledby="recettes" className="recipes">
            <h2 id="recettes">À cuisiner en premier</h2>
            <Suggestions data={suggestions} />
          </section>

          <aside className="side">
            <section aria-labelledby="ajouter-titre" id="ajouter">
              <h2 id="ajouter-titre">Ajouter un aliment</h2>
              <AddItemForm onAdd={add} />
            </section>
            <section aria-labelledby="inventaire">
              <h2 id="inventaire">Dans le frigo</h2>
              <Inventory items={items} onRemove={remove} />
            </section>
          </aside>
        </div>
      </main>

      <footer className="footer">
        React → Istio Gateway (HTTPS) → pantry-service (REST, PostgreSQL) → recipe-service (gRPC, mTLS)
      </footer>

      <div className="toast" role="status" aria-live="polite">{notice}</div>
    </div>
  )
}
