import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyArrowPatch

fig, ax = plt.subplots(figsize=(12.5, 7.8))
ax.set_xlim(0, 12.5)
ax.set_ylim(0, 8)
ax.axis("off")

def box(cx, cy, w, h, title, lines, fc="#eef3fb", ec="#2c4a7c", italic_title=False):
    x, y = cx - w / 2, cy - h / 2
    rect = mpatches.FancyBboxPatch((x, y), w, h,
                                    boxstyle="round,pad=0.02,rounding_size=0.06",
                                    linewidth=1.4, edgecolor=ec, facecolor=fc)
    ax.add_patch(rect)
    ax.plot([x, x + w], [y + h - 0.42, y + h - 0.42], color=ec, linewidth=1.0)
    style = "italic" if italic_title else "normal"
    ax.text(cx, y + h - 0.24, title, ha="center", va="center",
            fontsize=10.5, fontweight="bold", fontstyle=style)
    body = "\n".join(lines)
    ax.text(cx, y + (h - 0.42) / 2, body, ha="center", va="center", fontsize=8.2)
    return (cx, y, y + h, x, x + w)

def arrow_up(x1, y1, x2, y2, style="-|>", color="#2c4a7c", ls="solid", lw=1.3, mutation=16):
    arr = FancyArrowPatch((x1, y1), (x2, y2), arrowstyle=style, color=color,
                           linestyle=ls, linewidth=lw, mutation_scale=mutation,
                           connectionstyle="arc3,rad=0.0")
    ax.add_patch(arr)

# Interfaces (top)
loc = box(1.4, 7.3, 2.2, 0.9, "<<interface>>\nLocatable", ["getPosition()"], fc="#fff6e3", ec="#8a6d1a")
edi = box(4.2, 7.3, 2.4, 0.9, "<<interface>>\nEdible", ["getEnergyValue()", "markConsumed()"], fc="#fff6e3", ec="#8a6d1a")
mov = box(9.0, 7.3, 2.8, 0.9, "<<interface>>\nMovementStrategy", ["nextMove(self, world)"], fc="#fff6e3", ec="#8a6d1a")

# Organism (abstract)
org = box(3.0, 5.7, 3.6, 1.4, "Organism  (abstract)", [
    "- position: Position", "- alive: boolean  - age: int",
    "+ act(World) : void  {abstract}", "+ getColor()/getSymbol() {abstract}"
], fc="#e8f0ff")

# Plant
pla = box(1.2, 3.8, 2.3, 1.3, "Plant", [
    "implements Edible", "+ act(World)", "- trySpread(World)"
], fc="#e9f7ea", ec="#2f6d31")

# Animal (abstract)
ani = box(4.6, 3.8, 3.0, 1.5, "Animal  (abstract)", [
    "implements Edible", "- energy, maxAge, ...",
    "+ act(World)  {final, template method}",
    "# forage(World)  {abstract}",
    "# tryReproduce(World)  {abstract}"
], fc="#e8f0ff")

# Herbivore / Carnivore
herb = box(3.4, 1.7, 2.3, 1.3, "Herbivore", [
    "eats Plant", "+ forage(World)", "+ tryReproduce(World)"
], fc="#eaf0fc", ec="#2c4a7c")
carn = box(6.4, 1.7, 2.3, 1.3, "Carnivore", [
    "eats Herbivore", "+ forage(World)", "+ tryReproduce(World)"
], fc="#fdeaea", ec="#8a2020")

# World / Factory / CircularBuffer / Strategies (right column)
world = box(9.1, 5.5, 3.0, 1.7, "World", [
    "- grid: Map<Position,Organism>", "- organisms: List<Organism>",
    "+ tick()  + moveOrganism()",
    "+ <T> findNearest(pos,Class<T>,r)",
    "+ <T> countOfType(Class<T>)"
], fc="#f5eefb", ec="#5a2e82")

fact = box(9.1, 3.6, 3.0, 0.9, "OrganismFactory", ["+ createPlant/Herbivore/Carnivore()"], fc="#f5eefb", ec="#5a2e82")

cbuf = box(6.6, 5.6, 2.2, 1.0, "CircularBuffer<T>", ["ring buffer,", "implements Iterable<T>"], fc="#f0f0f0", ec="#555")

rws = box(10.8, 2.5, 2.6, 0.8, "RandomWalkStrategy", [], fc="#fff6e3", ec="#8a6d1a")
fss = box(10.8, 1.15, 2.9, 1.0, "FleeOrSeekStrategy", ["Class<? extends Organism>", "threat / food tokens"], fc="#fff6e3", ec="#8a6d1a")

# Inheritance arrows (hollow triangle = "-|>")
arrow_up(3.0, org[1], 1.2, pla[2])
arrow_up(3.0, org[1], 4.6, ani[2])
arrow_up(4.6, ani[1], 3.4, herb[2])
arrow_up(4.6, ani[1], 6.4, carn[2])

# Realizes (dashed) interface implementations
arrow_up(3.0, org[2], 1.4, loc[1], ls=(0, (4, 2)))
arrow_up(1.2, pla[2]+0.0, 4.2, edi[1], ls=(0, (4, 2)))
arrow_up(4.6, ani[2], 4.2, edi[1], ls=(0, (4, 2)))
arrow_up(4.6, ani[2]+1.2, 9.0, mov[1], ls=(0, (4, 2)))

arrow_up(7.55, 1.9, 9.5, 2.5, ls=(0, (4, 2)))
arrow_up(7.55, 1.5, 9.35, 1.15, ls=(0, (4, 2)))

ax.text(0.1, 0.35, "solid arrow = inheritance (extends)      dashed arrow = interface implementation / uses", fontsize=8, color="#444")
ax.text(0.1, 0.05, "Ecosystem Simulation - class relationships (ecosim package)", fontsize=9, color="#444", fontweight="bold")

plt.tight_layout()
plt.savefig("/home/claude/ecosim-project/design/class_diagram.png", dpi=200, bbox_inches="tight")
print("saved")
