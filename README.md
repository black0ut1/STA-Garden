# STA Garden
Implementation of few static traffic assignment algorithms and some visualization. This project uses the [TNTP](https://github.com/bstabler/TransportationNetworks)
format as input and output (there are already some networks in `data`).

## Implemented features

### Algorithms
Hierarchy of STA algorithms is represented using inheretance, where *Algorithm* is the top-most superclass.

Link-based algorithms:
- Method of Successive Averages (MSA)
- Frank-Wolfe (FW)
- Conjugate Frank-Wolfe (CFW)
- Biconjugate Frank-Wolfe (BFW)
- Fukushima Frank-Wolfe (FFW)

Path-based algorithms:
- Gradient Projection (GP), incomplete
- Projected Gradient (PG), incomplete

Bush-based algorithms:
- Origin-Based Assignment (OBA), incomplete
- Dial's B algorithm, wierd behavior (bugged?)
- Improved Traffic Assignment by Paired Alternative Segments (iTAPAS), including postprocessing procedure for solving proportionality/entropy maximization.

And All-Or-Nothing (AON) algorithm for initial flows.


### Convergence criterions
All criterions are implemented in class *Convergence* which has *Builder* inner class for convinient adding of multiple criterions.
If multiple convergence criterions are used and they share some computations (typically shortest path travel time), those common values
are precomputed so that they are not computed twice.

Implemented criterions are:
- Beckmann function (the objective function of static traffic assignment)
- Total system travel time (TSTT)
- Gap
- Three different implementations of relative gap (RG), see Stephen Boyles - Transport Network Analysis, page 147
- Average Excess Cost (AEC)


### Visualization
- Graph with logarithmic scale for plotting RG/AEC
- Graph showing the traffic network with edges colored according to their congestion
