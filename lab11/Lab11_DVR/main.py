import sys
from typing import Dict, List, Tuple

INF = 100


class Node:
    def __init__(self, node_id: int, neighbors: Dict[int, int]):
        self.id = node_id
        self.neighbors = neighbors.copy() 
        self.routing_table: Dict[int, Tuple[int, int]] = {}
        self.dirty = False
        self._init_table()

    def _init_table(self):
        self.routing_table[self.id] = (0, -1)
        for nb, cost in self.neighbors.items():
            self.routing_table[nb] = (cost, nb)

    def get_vector(self) -> Dict[int, int]:
        vector = {}
        for dest, (cost, _) in self.routing_table.items():
            vector[dest] = cost
        return vector

    def update_from_neighbor(self, from_node: int, vector: Dict[int, int]) -> bool:
        cost_to_neighbor = self.neighbors[from_node]
        changed = False

        for dest, neighbor_cost in vector.items():
            new_cost = cost_to_neighbor + neighbor_cost
            if dest not in self.routing_table:
                self.routing_table[dest] = (INF, -1)

            current_cost, current_next = self.routing_table[dest]

            if (new_cost < current_cost) or (current_next == from_node and new_cost > current_cost):
                self.routing_table[dest] = (new_cost, from_node)
                changed = True
        return changed


class Simulator:
    def __init__(self, nodes: Dict[int, Node]):
        self.nodes = nodes
        self.message_queue: List[Tuple[int, int, Dict[int, int]]] = []

    def _send_vectors(self):
        for node_id, node in self.nodes.items():
            if node.dirty:
                vector = node.get_vector()
                for neighbor_id in node.neighbors.keys():
                    self.message_queue.append((node_id, neighbor_id, vector))
                node.dirty = False 

    def _process_messages(self) -> bool:
        any_change = False
        while self.message_queue:
            sender, receiver, vector = self.message_queue.pop(0)
            node = self.nodes[receiver]
            changed = node.update_from_neighbor(sender, vector)
            if changed:
                node.dirty = True
                any_change = True
        return any_change

    def run(self, max_rounds=100) -> int:
        for node in self.nodes.values():
            node.dirty = True

        round_count = 0
        while round_count < max_rounds:
            self._send_vectors()
            changed = self._process_messages()
            if not changed:
                break
            round_count += 1
        return round_count

    def print_routing_tables(self):
        print("\n=== Routing tables ===")
        for node_id, node in self.nodes.items():
            print(f"\nNode {node_id}:")
            print(f"  {'Dest':>10} | {'Cost':>9} | {'Next':>15}")
            for dest, (cost, nxt) in sorted(node.routing_table.items()):
                if nxt == -1:
                    nxt_str = "-"
                else:
                    nxt_str = str(nxt)
                print(f"  {dest:>10} | {cost:>9} | {nxt_str:>15}")

    def change_link_cost(self, a: int, b: int, new_cost: int):
        old_cost = self.nodes[a].neighbors[b]
        print(f"\nChanging link {a}<->{b} from {old_cost} to {new_cost}")
        self.nodes[a].neighbors[b] = new_cost
        self.nodes[b].neighbors[a] = new_cost

        self.nodes[a].dirty = True
        self.nodes[b].dirty = True

        self.nodes[a].routing_table[b] = (new_cost, b)
        self.nodes[b].routing_table[a] = (new_cost, a)

def build_network() -> Dict[int, Node]:
    neighbors = {
        0: {1: 1, 2: 3, 3: 7},
        1: {0: 1, 2: 1},
        2: {0: 3, 1: 1, 3: 2},
        3: {0: 7, 2: 2}
    }
    nodes = {}
    for nid, neigh in neighbors.items():
        nodes[nid] = Node(nid, neigh)
    return nodes


def main():
    nodes = build_network()
    sim = Simulator(nodes)

    print("\n--- Init run ---")
    rounds = sim.run()
    print(f"Took {rounds} rounds to get there")
    sim.print_routing_tables()

    sim.change_link_cost(1, 2, 5)  
    print("\n--- After change ---")
    rounds = sim.run()
    print(f"Took {rounds} rounds to get there")
    sim.print_routing_tables()


if __name__ == "__main__":
    main()