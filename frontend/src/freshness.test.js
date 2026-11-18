import { describe, expect, it } from 'vitest'
import { dayLabel, formatQuantity, headline, isoDateIn, timelineColumns } from './freshness.js'

const item = (name, daysLeft) => ({ id: name, name, daysLeft })

describe('headline', () => {
  it('met en avant les aliments urgents', () => {
    expect(headline([item('Lait', 0), item('Tomate', 1), item('Riz', 90)]))
      .toBe('2 aliments à sauver avant demain soir.')
  })
  it('ignore les périmés et parle de la semaine sinon', () => {
    expect(headline([item('Épinards', -1), item('Courgette', 3)]))
      .toBe("Rien d'urgent. 1 aliment à cuisiner cette semaine.")
  })
  it('gère le frigo vide', () => {
    expect(headline([])).toBe('Votre frigo est vide.')
  })
})

describe('timelineColumns', () => {
  it('place chaque aliment dans une seule colonne', () => {
    const items = [item('a', -2), item('b', 0), item('c', 1), item('d', 4), item('e', 30)]
    const cols = timelineColumns(items, new Date(2026, 0, 10))
    expect(cols).toHaveLength(9)
    expect(cols.flatMap((c) => c.items)).toHaveLength(items.length)
    expect(cols.find((c) => c.key === 'later').items[0].name).toBe('e')
    expect(cols.find((c) => c.key === 'd4').title).toBe('mer. 14')
  })
})

describe('formats', () => {
  it('libellés de jours', () => {
    expect(dayLabel(-3)).toBe('périmé depuis 3 jours')
    expect(dayLabel(0)).toBe("aujourd'hui")
    expect(dayLabel(5)).toBe('dans 5 jours')
  })
  it('quantités et dates', () => {
    expect(formatQuantity('400.00', 'g')).toBe('400 g')
    expect(isoDateIn(3, new Date(2026, 0, 30))).toBe('2026-02-02')
  })
})
