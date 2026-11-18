export default function Suggestions({ data }) {
  if (!data) return <p className="muted">Recherche de recettes…</p>
  if (!data.available) {
    return (
      <p className="alert">
        Les suggestions de recettes sont indisponibles pour l'instant. Votre frigo reste consultable ;
        elles reviendront dès que le service de recettes répondra.
      </p>
    )
  }
  if (data.suggestions.length === 0) {
    return <p className="muted">Ajoutez quelques aliments pour recevoir des idées de recettes.</p>
  }
  return (
    <ol className="recipe-list">
      {data.suggestions.map(({ recipe, score, usedIngredients, missingIngredients, rescuedIngredients }) => (
        <li key={recipe.id} className="recipe">
          <div className="recipe-head">
            <span className="recipe-emoji" aria-hidden="true">{recipe.emoji}</span>
            <div>
              <h3>{recipe.title}</h3>
              <p className="recipe-meta">
                {recipe.prepMinutes} min
                {rescuedIngredients.length > 0 && (
                  <>, sauve {rescuedIngredients.join(', ').toLowerCase()}</>
                )}
              </p>
            </div>
            <meter className="score" min="0" max="100" value={score} title={`Pertinence ${score}/100`}>{score}/100</meter>
          </div>
          <ul className="ingredients" aria-label="Ingrédients">
            {usedIngredients.map((name) => (
              <li key={name} className={rescuedIngredients.includes(name) ? 'ing ing-rescued' : 'ing ing-have'}>{name}</li>
            ))}
            {missingIngredients.map((name) => (
              <li key={name} className="ing ing-missing"><span className="sr-only">à acheter : </span>{name}</li>
            ))}
          </ul>
          <details>
            <summary>Voir les étapes</summary>
            <ol className="steps">
              {recipe.steps.map((step) => <li key={step}>{step}</li>)}
            </ol>
          </details>
        </li>
      ))}
    </ol>
  )
}
