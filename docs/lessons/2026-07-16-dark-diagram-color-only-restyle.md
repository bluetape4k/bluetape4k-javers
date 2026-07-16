# Dark Diagram Restyles Must Preserve Card Shape

## Context

The Javers manual diagrams were asked to adopt the dark manual palette while
retaining the established card style. The renderer instead added a thick accent
rail to every card, so the change altered both color and shape.

## Root Cause

The shared `card()` helper emitted two visual edges: the normal colored card
outline and a separate `8px` accent rectangle on the left. Because the helper
was shared, the unintended decoration appeared 30 times across four diagrams.

## Decision

- Treat a color-only restyle as a palette substitution, not permission to add
  borders, rails, dividers, shadows, or other geometry.
- Keep cards as one rounded rectangle with one colored outline unless the user
  explicitly approves a structural style change.
- Validate every `card-group` has no extra rectangle decoration.
- Scan and inspect the complete related diagram set whenever a shared visual
  helper changes.

## Verification

```bash
ruby scripts/manual/validate_diagrams.rb
rg 'width="8"' docs/manual/assets -g '*.svg'
git diff --check
```

The four affected PNGs must also be opened at original size after the final
render. Automated SVG checks do not replace that visual review.
