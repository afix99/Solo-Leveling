#!/usr/bin/env python3
"""Regenerates the review/*.txt bundles from the current source.

Run from anywhere:  python3 review/build-bundles.py

Asserts that every source file in the project lands in exactly one bundle, so
a newly added file fails the build rather than being silently left out."""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "review")
MAIN = "app/src/main/java/com/ascend/app"
TEST = "app/src/test/java/com/ascend/app"


def ls(pattern_dir, suffix=".kt", recursive=False):
    base = os.path.join(ROOT, pattern_dir)
    out = []
    if recursive:
        for dirpath, _, files in os.walk(base):
            for f in files:
                if f.endswith(suffix):
                    out.append(os.path.relpath(os.path.join(dirpath, f), ROOT))
    else:
        for f in os.listdir(base):
            p = os.path.join(base, f)
            if os.path.isfile(p) and f.endswith(suffix):
                out.append(os.path.relpath(p, ROOT))
    return sorted(out)


screens_all = ls(f"{MAIN}/ui/screens", recursive=True)
viewmodels = [f for f in screens_all if f.endswith("ViewModel.kt")]
screens = [f for f in screens_all if not f.endswith("ViewModel.kt")]

BUNDLES = [
    ("01-ENTRY-AND-CONFIG.txt",
     "APP ENTRY POINT, BUILD CONFIGURATION AND RESOURCES",
     """Where the app starts and how it is assembled. AscendApplication builds the
Room database and repository once and hands the same instances to every
screen and worker — this is the whole of the dependency injection story, on
purpose (no Hilt/Koin). MainActivity hosts a single Compose tree; there is
one other Activity in the app, for focus sessions.

The Gradle files show the full dependency list. Note what is absent: no
networking library, no JSON library, no DI, no image loading.""",
     ["app/src/main/AndroidManifest.xml",
      f"{MAIN}/AscendApplication.kt",
      f"{MAIN}/MainActivity.kt",
      "build.gradle.kts",
      "settings.gradle.kts",
      "gradle.properties",
      "app/build.gradle.kts",
      "app/proguard-rules.pro"] + ls("app/src/main/res", ".xml", recursive=True)),

    ("02-DOMAIN-GAME-LOGIC.txt",
     "DOMAIN — ALL GAME RULES (PURE KOTLIN, NO ANDROID IMPORTS)",
     """This is where every balance decision lives. Nothing here imports Android,
which is why all 129 unit tests target this package plus data/ai.

Read HunterLoadout.kt first — it is the type that unifies the systems. Every
payout in the app (XP earned, gold earned, penalty size, whether a streak
survives a miss) resolves through it, so adding a new modifier means editing
one file rather than threading a parameter through the repository.

If you are evaluating game balance or looking for exploits, everything you
need is in this bundle.""",
     ls(f"{MAIN}/domain")),

    ("03-DATABASE-LAYER.txt",
     "PERSISTENCE — ROOM ENTITIES, DAOS, DATABASE AND MIGRATIONS",
     """17 tables. Schema version 5 with four hand-written migrations
(v1->v2->v3->v4->v5); none is destructive and none has ever been executed
against a populated database. Migration correctness is priority 2 in the
review brief.

HunterProfileEntity is a singleton row (id = 0). Dates are stored as ISO-8601
strings, timestamps as epoch millis.""",
     ls(f"{MAIN}/data/db")),

    ("04-REPOSITORY.txt",
     "REPOSITORY — THE SINGLE DATA AND LOGIC SEAM",
     """Everything goes through here: ViewModels, WorkManager workers, the AI coach.
Nothing else touches a DAO.

The methods most worth scrutinising:
  setHabitCompleted()   — the XP/gold/streak/achievement award path. If XP can
                          be farmed by toggling, it happens here.
  runMidnightRollover() — penalties, gate progress, daily quest rotation. Runs
                          from a Worker. Guarded by lastEvaluatedDate.
  currentLoadout()      — assembles the HunterLoadout every payout depends on.
  buildCoachContext()   — the only place user data is shaped for the network.

This file is ~900 lines and is the most plausible candidate for splitting.""",
     ls(f"{MAIN}/data/repo")),

    ("05-AI-COACH.txt",
     "AI COACH — HTTP CLIENT, PROVIDER DETECTION, PROMPT CONSTRUCTION",
     """The only networked code in the app. Raw HttpURLConnection and platform
org.json; no Retrofit, no serialization library.

AiClient.kt — request/response for both OpenAI-shaped chat completions and
Gemini's native generateContent. extractErrorMessage() handles errors arriving
as either a JSON object or a JSON array; the array case was a real bug that
silently swallowed Google's error text for several debugging rounds.
completeAutoRecovering() handles a 404 on the configured model by fetching the
provider's live model list, picking a usable one, retrying and saving it.

CoachPrompts.kt — the safety guardrails (calorie floors, deficit ceiling,
injury and diagnosis rules) are constants folded into every system prompt,
including when the in-character "System voice" persona is enabled. Tests
assert the persona cannot displace them.

The key is supplied by the user, stored in Room, and sent only to the provider
the user chose. Nothing else leaves the device. Security review of this path
is priority 6 in the brief.""",
     ls(f"{MAIN}/data/ai")),

    ("06-UI-SHELL-AND-COMPONENTS.txt",
     "UI SHELL — NAVIGATION, THEME AND SHARED COMPONENTS",
     """AscendApp.kt is the navigation graph and app shell. It uses a Column rather
than a Scaffold so the custom dock reserves its own space, and applies
statusBarsPadding() at the root because the app draws edge-to-edge.

Note ViewModelUtils.kt: viewModel() scopes per NavBackStackEntry, so two
screens in the same feature get different ViewModel instances. That caused a
real bug (settings saved on one screen were invisible to the other) and is
why some state is read straight from the repository rather than cached in
init.

Material 3 is used as a primitive layer only — the visible chrome (SystemDock,
SystemWindow, GlassCard) is custom.""",
     [f"{MAIN}/ui/AscendApp.kt", f"{MAIN}/ui/ViewModelUtils.kt"]
     + ls(f"{MAIN}/ui/components") + ls(f"{MAIN}/ui/theme")),

    ("07-UI-SCREENS.txt",
     "UI — ALL SCREEN COMPOSABLES",
     """Every screen. TodayScreen is the one that matters most: it is the daily
surface and it deliberately collapses optional habits until the
non-negotiables are done (cognitive-load reasoning).

focus/ contains a separate Activity, because a focus session needs to survive
navigation and keep the screen awake independently of the main tree.

No Compose UI tests exist for any of this.""",
     screens + ls(f"{MAIN}/focus")),

    ("08-VIEWMODELS.txt",
     "VIEWMODELS — ALL STATE HOLDERS",
     """State exposed as StateFlow, mutations as suspend calls into the repository.
Priority 3 in the review brief is here: anything that could leak a coroutine,
double-fire on a rapid tap, or race between a collect and a write.

CoachViewModel is the most complex — it owns network calls, loading states,
and settings that must stay consistent across two screens that hold separate
instances of it.""",
     viewmodels),

    ("09-BACKGROUND-SERVICES.txt",
     "BACKGROUND — WORKMANAGER JOBS, NOTIFICATIONS, BOOT RESCHEDULE",
     """The midnight rollover worker is load-bearing: penalties, gate progress and
daily quest rotation all depend on it firing. If the OS defers it (Doze,
battery optimisation, force-stop) those are late or skipped. There is a
lastEvaluatedDate guard against double-counting but no catch-up pass for
days that were missed entirely — a known weak point.

Notifications are muted while a focus session is active (flow-state
reasoning). BootRescheduleReceiver re-arms everything after a reboot.""",
     ls(f"{MAIN}/notifications")),

    ("10-TESTS.txt",
     "TESTS — ALL 129 UNIT TESTS",
     """All of them are JVM unit tests on domain/ and data/ai. There are no
instrumented tests, no Compose UI tests and no Room migration tests.

One thing worth knowing: Android stubs org.json in unit tests, so every JSON
class silently returns defaults. The JSON parsing tests in AiClientTest
appeared to pass while executing nothing until a real org.json implementation
was added as a test dependency. Any test you propose that touches JSON needs
that dependency to mean anything.""",
     ls(TEST, recursive=True)),
]

# --- coverage check -----------------------------------------------------------
expected = set(ls(MAIN, ".kt", recursive=True)) | set(ls(TEST, ".kt", recursive=True))
expected |= set(ls("app/src/main/res", ".xml", recursive=True))
expected |= {"app/src/main/AndroidManifest.xml", "build.gradle.kts",
             "settings.gradle.kts", "gradle.properties", "app/build.gradle.kts",
             "app/proguard-rules.pro"}

covered = []
for _, _, _, files in BUNDLES:
    covered += files

dupes = {f for f in covered if covered.count(f) > 1}
assert not dupes, f"file in more than one bundle: {dupes}"
missing = expected - set(covered)
assert not missing, f"file in no bundle: {sorted(missing)}"
extra = set(covered) - expected
assert not extra, f"bundled a file that does not exist: {sorted(extra)}"

# --- write --------------------------------------------------------------------
os.makedirs(OUT, exist_ok=True)
BAR = "=" * 78

for name, title, preamble, files in BUNDLES:
    parts = [BAR, f"  ASCEND — {title}", f"  {len(files)} files", BAR, "",
             preamble.strip(), "", ""]
    for rel in files:
        with open(os.path.join(ROOT, rel), encoding="utf-8") as fh:
            body = fh.read()
        parts += [BAR,
                  f"  FILE:  {rel}",
                  f"  LINES: {len(body.splitlines())}",
                  BAR, "", body.rstrip("\n"), "", ""]
    parts += [BAR, f"  END OF {name}", BAR, ""]
    with open(os.path.join(OUT, name), "w", encoding="utf-8") as fh:
        fh.write("\n".join(parts))
    print(f"{name:34} {len(files):3} files  "
          f"{os.path.getsize(os.path.join(OUT, name)) // 1024:5} KB")

print(f"\n{len(expected)} files, all covered exactly once.")
