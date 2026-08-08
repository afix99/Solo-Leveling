# Review bundles

The full source of the app, flattened into plain-text files so it can be handed
to a code reviewer — human or AI — without cloning the repo.

**Start with [`00-REVIEW-BRIEF.txt`](00-REVIEW-BRIEF.txt).** It explains what the
app is, how it is put together, which design decisions were deliberate, and a
list of known weak points, so a reviewer doesn't spend its effort rediscovering
things already known.

| Bundle | Contents |
|---|---|
| `00-REVIEW-BRIEF.txt` | Context. Read first. |
| `01-ENTRY-AND-CONFIG.txt` | Manifest, Application, Activity, Gradle, resources |
| `02-DOMAIN-GAME-LOGIC.txt` | All game rules (pure Kotlin, fully unit-tested) |
| `03-DATABASE-LAYER.txt` | Entities, DAOs, database, all four migrations |
| `04-REPOSITORY.txt` | The single data/logic seam |
| `05-AI-COACH.txt` | HTTP client, provider detection, prompts |
| `06-UI-SHELL-AND-COMPONENTS.txt` | Navigation, theme, shared components |
| `07-UI-SCREENS.txt` | All screen composables |
| `08-VIEWMODELS.txt` | All state holders |
| `09-BACKGROUND-SERVICES.txt` | WorkManager jobs, notifications |
| `10-TESTS.txt` | All unit tests |

If a reviewer can only read part of it, the densest path is
**02 → 04 → 03 → 05** (~156 KB), which holds essentially all of the logic.
`07` is the largest bundle but the least dense — it is layout code.

Every file appears under a banner giving its full repository path and line
count, so findings can be reported as `domain/HunterLoadout.kt:214` rather than
"that one function".

## These are generated

They are a snapshot, not a source of truth, and they go stale as soon as the
code changes. Regenerate with:

    python3 review/build-bundles.py

The script asserts that every source file in the project lands in exactly one
bundle, so adding a file to the app and forgetting to re-run this fails loudly
rather than quietly shipping an incomplete export.

No API keys, credentials or personal data are included; test fixtures that
resemble keys are synthetic.
