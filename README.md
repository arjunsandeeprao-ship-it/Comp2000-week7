# Ecosystem Simulation — COMP2000

A grid-based predator–prey–plant ecosystem simulation built with **only standard JRE classes**
(Swing for the GUI, `java.util`/`java.util.stream` for the model). Plants grow and spread,
herbivores eat plants and flee carnivores, and carnivores hunt herbivores — the three populations
rise and fall in an emergent boom/bust cycle.

See `worksheet-7.md` for the worksheet answers and `design/program-design.pdf` for the full design
write-up (class list, diagram, and the reasoning behind the inheritance hierarchy, the Template
Method and Strategy patterns, the generics, and the exception handling).

## Build & run

Requires a JDK (developed against Java 21, but only uses language features safe on much older
versions too). No build tool, no external dependencies.

```bash
javac -d bin $(find src -name "*.java")

# Watch it run (Swing GUI):
java -cp bin ecosim.Main

# Or check the model logic headlessly (prints population counts every 20 ticks):
java -cp bin ecosim.SmokeTest
```

## Project layout

```
src/ecosim/              core model classes (Position, Organism, Plant, Animal, Herbivore,
                          Carnivore, World, CircularBuffer, OrganismFactory), plus the Swing GUI
                          (WorldPanel, SimulationFrame, Main) and SmokeTest
src/ecosim/strategy/      MovementStrategy and its two implementations (Strategy pattern)
src/ecosim/exceptions/    SimulationException hierarchy (InvalidPositionException,
                          OverpopulationException)
design/                   class diagram source + generated program-design.pdf
logbook/                  weekly log book template
worksheet-7.md            worksheet answers
```
