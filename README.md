# Evolving Random Graphs Generator
Generate random graphs that evolve over time.

### Initial Graph Generation
As base graph, we generate a two dimensional Euclidean Random Graph with a threshold distance of _0.025_
to connect two vertices.
Two vertices are connected over an edge, if the euclidean distance is less than the defined threshold.
In addition, we generate types, properties, and sources.

**Types** are generated using the `generateTypes()` function. 
The number of types is generated as follows: `random.nextGaussian() + ((MAX - MIN)/2)` 
with MIN and MAX as the minimum number of types and maximum number of types.
Furthermore, there is a hard cap, i.e., values greater than MAX are set to the MAX value and less than MIN are set to the MIN value.
Each type is then selected from the pool of types based on the probability 
`Math.max(0, Math.min(1, random.nextGaussian())) * (this.types.length - 1)`.

**Properties** and **sources** are selected with `numericToProperty(float number)` 
and `numericToSource(float number)`, based on the random numeric values assigned by the GraphStream library.


We initialize the graph with _10,000_ vertices with random coordinates.
After the vertices and edges are generated, we transform the numeric values to string values with the 
`transform()` method.
For each vertex, we add between `MIN = 0` and `MAX = 4` types from the pool of `T = (5|7|10)` types and 
select one source graph label from a pool of _50_ source graph label based on the numeric value of the vertex.
For each edge, we select one attribute from the pool of `P = (5|7|10)` attributes.
Furthermore, we assign the source graph label of the edges's source vertex as source graph for the edge 
with a probability of _95%_.
Thus, each edge has a probability of _0.5%_ to be defined in randomly selected source graph.

### Temporal Evolution
To emulate the temporal evolution, we randomly delete _30%_ of all vertices and, subsequently, add _40%_ 
randomly new generated vertices.
Some vertex identifiers may be reassigned, thus, resulting in a vertex modification rather than a new vertex.
For each evolution iteration, we employ a decay factor of _95%_, i.e., each iteration _5%_ less 
additions and deletion are generated.
In order to enforce a realistic vertex-edge distribution, in each iteration, the threshold factor is evaluated.
When there are more than 10-times more edges than vertices, the threshold is reduced.
When there are less than 3-times more edges than vertices, the threshold is increased.

