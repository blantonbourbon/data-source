# Refine advanced search and email export UI

## Goal
Make the advanced search area more compact so query results have more room, and simplify the email export UI so rarely changed email settings do not dominate the result-tools panel.

## Requirements
- Reduce the visual size of the advanced search filters in the shared query builder.
- Keep the advanced search controls readable and accessible while using less vertical space.
- Keep the export send action visible in the result-tools area.
- Keep the selected-row count visible near the export action.
- Move rarely changed email/export settings into a secondary standalone config area instead of making them always expanded.
- Preserve the current export behavior, recipient parsing, download behavior, and `/export/email` request contract.

## Acceptance Criteria
- [ ] Advanced search filters use smaller typography and denser spacing than the current UI.
- [ ] The advanced filter panel takes less vertical space than before.
- [ ] The data-query screen keeps a visible send-to-email button and selected-row summary even when detailed email settings are collapsed.
- [ ] Users can still edit recipient and export-format settings from a dedicated config area.
- [ ] Existing CSV/XLSX download and export-email request behavior still works.
- [ ] Targeted frontend tests pass for the updated behavior.

## Technical Notes
- Expected files:
  - `frontend-monorepo/projects/multi-module-app/src/app/shared/components/query-builder/query-builder.component.scss`
  - `frontend-monorepo/projects/multi-module-app/src/app/features/data-query/data-query.component.html`
  - `frontend-monorepo/projects/multi-module-app/src/app/features/data-query/data-query.component.scss`
  - `frontend-monorepo/projects/multi-module-app/src/app/features/data-query/data-query.component.ts`
  - `frontend-monorepo/projects/multi-module-app/src/app/features/data-query/data-query.component.spec.ts`
- Assumption:
  - "email config can make it standalone" means the detailed export settings should live in a separate toggleable section while the primary send action and selection summary remain visible.
