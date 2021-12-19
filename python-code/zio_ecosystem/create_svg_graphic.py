"""
Reads a Graphviz DOT file from STDIN, and writes an SVG graphic to STDOUT.
"""
import sys
from typing import Iterable, TextIO

import dagviz
import pygraphviz
import networkx as nx
from networkx.drawing import nx_agraph

def main() -> None:
    graph = load_graph(sys.stdin)
    render(graph)


def load_graph(file_io: TextIO) -> nx.DiGraph:
    text_contents = file_io.read()
    return nx_agraph.from_agraph(pygraphviz.AGraph(text_contents))


def custom_ordering(graph: nx.DiGraph) -> Iterable[int]:
    """
    Order nodes by:
    1. Length of the longest path within each weakly connected component.
    2. Topological layer within each connected component. (A generalization of
        topological sort order.)
    3. Out-degree.
    4. Lexicographic order to break ties.
    """

    connected_components = [
        graph.subgraph(node_set)
        for node_set in nx.weakly_connected_components(graph)
    ]

    connected_components.sort(
        key=lambda component: (
            nx.dag_longest_path_length(component),
            next(iter(component)).lower()
        )
    )

    for component in connected_components:
        subgraph = graph.subgraph(component)
        for generation in nx.topological_generations(subgraph):
            generation_sorted = sorted(
                generation,
                key=lambda n: (subgraph.out_degree(n), n)
            )
            yield from generation_sorted


def render(graph: nx.DiGraph) -> None:
    abstract_plot = dagviz.make_abstract_plot(
        graph,
        order=lambda g: list(custom_ordering(g))
    )
    r = dagviz.render(abstract_plot, dagviz.style.metro.svg_renderer())
    print(r)


if __name__ == "__main__":
    main()
