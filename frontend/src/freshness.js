// Règles d'affichage partagées (pures, testées dans freshness.test.js).

export const CATEGORIES = {
  VEGETABLE: 'Légumes',
  FRUIT: 'Fruits',
  DAIRY: 'Crèmerie',
  MEAT: 'Viande',
  FISH: 'Poisson',
  GRAIN: 'Épicerie',
  OTHER: 'Autre',
}

const plural = (n, word) => `${n} ${word}${n > 1 ? 's' : ''}`

/** Titre principal : dit en une phrase ce qu'il faut faire aujourd'hui. */
export function headline(items) {
  if (items.length === 0) return 'Votre frigo est vide.'
  const urgent = items.filter((i) => i.daysLeft >= 0 && i.daysLeft <= 1).length
  const soon = items.filter((i) => i.daysLeft >= 2 && i.daysLeft <= 4).length
  if (urgent > 0) return `${plural(urgent, 'aliment')} à sauver avant demain soir.`
  if (soon > 0) return `Rien d'urgent. ${plural(soon, 'aliment')} à cuisiner cette semaine.`
  return 'Tout est frais. Rien ne presse.'
}

export function dayLabel(daysLeft) {
  if (daysLeft < -1) return `périmé depuis ${-daysLeft} jours`
  if (daysLeft === -1) return 'périmé depuis hier'
  if (daysLeft === 0) return "aujourd'hui"
  if (daysLeft === 1) return 'demain'
  return `dans ${daysLeft} jours`
}

/**
 * Colonnes de la frise : périmé, aujourd'hui, demain, J+2…J+6, plus tard.
 * Chaque aliment tombe dans exactement une colonne.
 */
export function timelineColumns(items, today = new Date()) {
  const weekday = new Intl.DateTimeFormat('fr-FR', { weekday: 'short', day: 'numeric' })
  const cols = [
    { key: 'expired', title: 'Périmé', match: (d) => d < 0 },
    { key: 'd0', title: "Aujourd'hui", match: (d) => d === 0 },
    { key: 'd1', title: 'Demain', match: (d) => d === 1 },
  ]
  for (let n = 2; n <= 6; n++) {
    const date = new Date(today)
    date.setDate(today.getDate() + n)
    cols.push({ key: `d${n}`, title: weekday.format(date), match: (d) => d === n })
  }
  cols.push({ key: 'later', title: 'Plus tard', match: (d) => d > 6 })
  return cols.map(({ match, ...col }) => ({ ...col, items: items.filter((i) => match(i.daysLeft)) }))
}

export function formatQuantity(quantity, unit) {
  const n = Number(quantity)
  return `${Number.isInteger(n) ? n : n.toLocaleString('fr-FR')} ${unit}`
}

export function isoDateIn(days, from = new Date()) {
  const d = new Date(from)
  d.setDate(d.getDate() + days)
  const pad = (x) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
