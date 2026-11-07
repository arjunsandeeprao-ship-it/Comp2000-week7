# COMP2000 Log Book — Ecosystem Simulation (Weeks 8–13)

**Student:** Arjun Sandeep Rao

---

## Week 8 — Observer pattern

**Date:** 27 September – 3 October

**What I did:**
Added a `SimulationListener` interface so `World` can notify other classes when a tick finishes. The GUI
now listens to the World instead of refreshing itself manually after every tick.

**What I learned / found difficult:**
Learned how the Observer pattern removes the dependency between `World` and the GUI. The tricky part was
Reset — a new World has no listeners, so the GUI has to register itself again.

**How this connects to earlier/later weeks:**
Builds on the `World.tick()` loop from Week 4, and made it possible to run the World without a GUI in Week 11.

---

## Week 9 — Streams and lambdas

**Date:** 4 – 10 October

**What I did:**
Rewrote the neighbour searches and the `forage()` methods using streams, and added `PopulationStats`, which
counts every species in one pass and uses a custom `Collector` to summarise animal energy.

**What I learned / found difficult:**
Writing my own `Collector` was hard at first — I had to understand what the supplier, accumulator, combiner
and finisher each do, especially the combiner, which only matters for parallel streams.

**How this connects to earlier/later weeks:**
Extends the stream style already used in `findNearest`/`countOfType` (Week 4), and set up the parallel work in Week 11.

---

## Week 10 — Seasons

**Date:** 11 – 17 October

**What I did:**
Added a `Season` enum. Every 50 ticks the season changes, which speeds up or slows down how fast plants
spread. The grid colour changes with the season.

**What I learned / found difficult:**
Learned that enums can hold data and behaviour, not just names. Seasons made the populations swing much
harder, which made the simulation less stable.

**How this connects to earlier/later weeks:**
First time Plant (Week 2) depended on the state of the whole world. The bigger swings led to the problem I found in Week 12.

---

## Week 11 — Parallel batch experiment

**Date:** 18 – 24 October

**What I did:**
Wrote `BatchExperiment`, which runs the simulation from 100 different seeds at the same time using a
parallel stream and summarises how often each species survives.

**What I learned / found difficult:**
Learned how to make parallel code thread-safe without locks — each run uses its own World and nothing is
shared between threads.

**How this connects to earlier/later weeks:**
Only possible because of the Observer (Week 8) and collectors (Week 9).

---

## Week 12 — Fixing the ecosystem collapse

**Date:** 25 – 31 October

**What I did:**
The batch experiment showed that every run ended with all animals extinct. I fixed a bug where newborns got
more energy than their parent paid, and added migration so a species can come back from the edge of the map
after dying out.

**What I learned / found difficult:**
Learned that testing with one seed isn't enough — I had been tuning the simulation by watching one run,
which gave the wrong picture.

**How this connects to earlier/later weeks:**
The fix was easy to make because all organism creation is in `OrganismFactory` (Week 5).

---

## Week 13 — Diagrams and final submission

**Date:** 1 – 8 November

**What I did:**
Made the design pattern diagrams, completed Worksheet 2, and committed each changed file separately to GitHub.

**What I learned / found difficult:**
Explaining the trade-offs of each pattern was harder than describing what they do.

**How this connects to earlier/later weeks:**
Brings together everything from Weeks 1–12 for the final submission.
