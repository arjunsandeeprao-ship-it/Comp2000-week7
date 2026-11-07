# COMP2000 Log Book — Ecosystem Simulation (Weeks 8–13)

**Student:** Arjun Sandeep Rao (60846534)

Continues the Weeks 1–7 log book submitted with Worksheet 1.

---

## Week 8 — Observer pattern: decoupling World from the GUI

**Date:** 27 September – 3 October

**What I did:**
Added a `SimulationListener` interface (`onTickCompleted`, plus a default `onExtinction`) and gave `World`
an `addListener`/`removeListener` API backed by a `CopyOnWriteArrayList`. `World.tick()` now finishes by
calling `notifyListeners()`. `SimulationFrame.onTick` shrank to a single `world.tick()` call — the
repaint and stats refresh happen in a listener the frame registers on itself.

**What I learned / found difficult:**
Before this, the GUI had to remember to refresh three things after every tick, and the Step button and
the Timer both went through that same hand-written sequence. Moving to Observer meant `World` no longer
needs to know a GUI exists at all. The tricky part was Reset: a fresh `World` has no listeners, so the
frame has to re-register on every new world, otherwise the screen would silently stop updating.

**How this connects to earlier/later weeks:**
Builds directly on the `World.tick()` loop from Week 4. It is what made Week 11 possible — a `World`
with no GUI attached can now run headless, many at a time, with each run attaching its own observer.

---

## Week 9 — Streams and lambdas throughout the model

**Date:** 4 – 10 October

**What I did:**
Rewrote `World.getNeighbours`/`freeNeighbours` as stream pipelines over the eight directions, rewrote
`Herbivore.forage`/`Carnivore.forage` as `filter → map → findFirst → ifPresent`, and made
`SimulationListener` a `@FunctionalInterface` so `SmokeTest` can register its row-printer as a lambda.
Added `PopulationStats`: `countBySpecies` (one `groupingBy` pass instead of three `countOfType` passes)
and a hand-written `Collector` (`energyCollector`) that summarises animal energy, including how many are
close to starving.

**What I learned / found difficult:**
Writing a `Collector` by hand forced me to understand what the four parts (supplier, accumulator,
combiner, finisher) are for — especially the combiner, which only matters for parallel streams.
`groupingBy` silently leaves out species with zero members, so after a crash the map would have had no
entry for that species and the stats label would have unboxed a `null`; I fill missing keys with 0.

**How this connects to earlier/later weeks:**
The Week 4 generic methods (`findNearest`, `countOfType`) were already streams; this week extended that
style to the rest of the model. The custom collector's combiner was written with Week 11's parallel
experiment in mind.

---

## Week 10 — Seasons

**Date:** 11 – 17 October

**What I did:**
Added a `Season` enum (SPRING, SUMMER, AUTUMN, WINTER) with behaviour: each season scales the plant
spread chance and the ambient seeding chance. `World` advances the season every 50 ticks; `Plant.act`
asks `world.getSeason()` how likely it is to spread; `WorldPanel` tints the grid with the season's colour
and the stats line shows the current season.

**What I learned / found difficult:**
Putting the multipliers on the enum constants (instead of `if (season == WINTER)` checks in `Plant` and
`World`) kept the season knowledge in one place. The hard part was that seasons made the populations
swing much harder: herbivores boomed in spring and then crashed.

**How this connects to earlier/later weeks:**
Plants were the simplest species back in Week 2; this was the first time their behaviour depended on
global world state. The stronger swings directly caused the Week 12 investigation.

---

## Week 11 — Parallel batch experiment

**Date:** 18 – 24 October

**What I did:**
Wrote `BatchExperiment`: runs the whole simulation from 100 different seeds using
`IntStream.range(0, runs).parallel().mapToObj(seed -> runOne(seed, ticks))`, then summarises the
results with collectors — how many runs still have animals alive, the share of ticks where all three
species coexist, average number of crashes, and which season crashes happen in.

**What I learned / found difficult:**
Thread safety came from design rather than locks: each run builds its own `World` and its own
`Random`, the per-run observer's lists are created inside `runOne`, and results are combined only by
collectors. I had to check there was no static mutable state anywhere in the model before trusting it.

**How this connects to earlier/later weeks:**
Only possible because of Week 8 (headless World with observers) and Week 9 (collectors). Its first real
run revealed the problem I spent Week 12 on.

---

## Week 12 — Finding out the ecosystem always collapsed

**Date:** 25 – 31 October

**What I did:**
The batch experiment showed that **0 out of the seeded runs** had any animals left after 600 ticks — the
original simulation (even before seasons) always ended in total extinction; the Week 7 claim that ambient
plant seeding "keeps it running for hundreds of ticks" was true for one lucky seed, not in general.
I found two causes:

1. An energy bug: a newborn got the full `INITIAL_ENERGY` (40) while the parent only paid
   `REPRODUCTION_COST` (30), so every birth created energy from nothing. I added
   `OrganismFactory.createHerbivore(Position, int)`/`createCarnivore(Position, int)` so a newborn carries
   exactly what its parent paid.
2. The grid is a sealed box: once one crash removes a species, it can never come back. I added migration
   (`World.maybeMigrate`): while a species is locally extinct, there is a 10% chance per tick that one
   animal arrives on a free edge cell.

I also tried predator and grazing "satiation" (not eating when full). The batch experiment showed it did
not help, so I removed it rather than leaving dead tuning code in.

**What I learned / found difficult:**
A single run is an anecdote — I had been tuning against one seed. Having a parallel experiment turned
"it looks OK" into a measurable number I could compare before and after each change.

**How this connects to earlier/later weeks:**
The energy fix only touched one class plus two call sites because of the Week 5 Simple Factory. The
extinction events are delivered through the Week 8 observer, and the GUI now shows the last crash in the
title bar instead of a pop-up, because crashes are now routine.

---

## Week 13 — Diagrams, worksheet, submission

**Date:** 1 – 8 November

**What I did:**
Produced `design/design-patterns.pdf` (one page per pattern: Template Method, Strategy, Simple Factory,
Observer, with roles, key interaction and trade-off), completed Worksheet 2, and committed each changed
or new file as its own commit so the history shows what each change was for.

**What I learned / found difficult:**
Drawing the Observer page made me notice that the order observers run in is not guaranteed — fine for
this project, but worth stating as a trade-off.

**How this connects to earlier/later weeks:**
Ties Weeks 8–12 back to the Week 7 design write-up: two of the four patterns were there in Week 7; the
Observer pattern and the factory's energy-conserving overloads are new since then.
