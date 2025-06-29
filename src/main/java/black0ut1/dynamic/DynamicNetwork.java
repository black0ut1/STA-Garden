package black0ut1.dynamic;

import black0ut1.data.network.Network;
import black0ut1.data.tuple.Pair;
import black0ut1.dynamic.loading.link.Connector;
import black0ut1.dynamic.loading.link.Link;
import black0ut1.dynamic.loading.link.LTM;
import black0ut1.dynamic.loading.node.*;
import black0ut1.util.Util;

/**
 * Class representing the traffic network in DNL.
 * <p>
 * To split the functionality, a zone is represented by origin,
 * destination, intersection and two connectors. The origin is a
 * virtual node and serves as source of flow of this zone. Destination
 * is also virtual and serves as the sink. Origin and destination are
 * connected to the physical intersection using connectors - virtual
 * links with zero travel time. For some zone i ({@code i in
 * [0, zones)}), all these parts are on index i in relevant arrays.
 */
public class DynamicNetwork {
	
	public final Intersection[] intersections;
	public final Origin[] origins;
	public final Destination[] destinations;
	/** The above three arrays merged into one for convenient use. */
	public final Node[] allNodes;
	
	public final Link[] links;
	public final Connector[] originConnectors;
	public final Connector[] destinationConnectors;
	/** The above three arrays merged into one for convenient use. */
	public final Link[] allLinks;
	
	public DynamicNetwork(Intersection[] intersections, Origin[] origins, Destination[] destinations,
						  Link[] links, Connector[] originConnectors, Connector[] destinationConnectors) {
		this.intersections = intersections;
		this.origins = origins;
		this.destinations = destinations;
		this.allNodes = Util.concat(Node.class, intersections, origins, destinations);
		
		this.links = links;
		this.originConnectors = originConnectors;
		this.destinationConnectors = destinationConnectors;
		this.allLinks = Util.concat(Link.class, links, originConnectors, destinationConnectors);
	}
	
	/**
	 * Sums up the total inflow and outflow over all links.
	 * @param time The time in which the flows are summed up. Must not
	 * be higher than the time the DNL is currently in.
	 * @return Pair (total inflow, total outflow).
	 */
	public Pair<Double, Double> getTotalInflowOutflow(int time) {
		double totalInflow = 0;
		double totalOutflow = 0;
		
		for (Link link : allLinks) {
			totalInflow += link.inflow[time].totalFlow;
			totalOutflow += link.outflow[time].totalFlow;
		}
		
		return new Pair<>(totalInflow, totalOutflow);
	}
	
	public static DynamicNetwork fromStaticNetwork(Network network, TimeDependentODM odm, double timeStep, int steps) {
		// 1. Create array of regular link and arrays of connectors -
		// links connecting virtual origins and destinations
		Link[] linkArray = new Link[network.edges];
		Connector[] originConnectors = new Connector[network.zones];
		Connector[] destinationConnectors = new Connector[network.zones];
		
		// 1.1. Create connectors
		for (int i = 0; i < network.zones; i++) {
			originConnectors[i] = new Connector(-1, steps);
			destinationConnectors[i] = new Connector(-1, steps);
		}
		
		// 1.2. Create classic links
		for (int i = 0; i < network.edges; i++) {
			Network.Edge link = network.getEdges()[i];
			
			// practical capacity used in STA is about 0.8 of actual capacity
			double freeFlowTime = Math.max(link.freeFlow, 0.1);
			double length = Math.max(link.length, 0.1);
			double capacity = 1.25 * link.capacity;
			double freeFlowSpeed = length / freeFlowTime;
			
			linkArray[i] = new LTM(i, timeStep, steps, length, capacity, 0, freeFlowSpeed, freeFlowSpeed / 3);
		}
		
		
		// 2. Create array of intersections and arrays of virtual
		// origins and destinations
		Intersection[] nodeArray = new Intersection[network.nodes];
		Origin[] originArray = new Origin[network.zones];
		Destination[] destinationArray = new Destination[network.zones];
		
		// 2.1. Create origins and destinations
		for (int i = 0; i < network.zones; i++) {
			originArray[i] = new Origin(i, originConnectors[i], odm);
			originConnectors[i].tail = originArray[i];
			
			destinationArray[i] = new Destination(i, steps, destinationConnectors[i]);
			destinationConnectors[i].head = destinationArray[i];
		}
		
		// 2.2. Count the number of incoming and outgoing links of
		// each intersections
		int[] outgoingLinkCounts = new int[network.nodes];
		int[] incomingLinkCounts = new int[network.nodes];
		for (Network.Edge link : network.getEdges()) {
			outgoingLinkCounts[link.tail]++;
			incomingLinkCounts[link.head]++;
		}
		for (int i = 0; i < network.zones; i++) {
			// intersections of zones have connectors
			outgoingLinkCounts[i]++;
			incomingLinkCounts[i]++;
		}
		
		// 2.3. Create intersections
		for (int i = 0; i < network.nodes; i++) {
			
			Link[] incomingLinks = new Link[incomingLinkCounts[i]];
			Link[] outgoingLinks = new Link[outgoingLinkCounts[i]];
			
			int j = 0;
			if (i < network.zones) // first incoming link of zone intersection is origin connector
				incomingLinks[j++] = originConnectors[i];
			for (Network.Edge link : network.backwardStar(i))
				incomingLinks[j++] = linkArray[link.index];
			
			j = 0;
			if (i < network.zones) // first outgoing link of zone intersection is destination connector
				outgoingLinks[j++] = destinationConnectors[i];
			for (Network.Edge link : network.forwardStar(i))
				outgoingLinks[j++] = linkArray[link.index];
			
			nodeArray[i] = new TampereUnsignalized(i, incomingLinks, outgoingLinks);
			for (Link incomingLink : incomingLinks)
				incomingLink.head = nodeArray[i];
			for (Link outgoingLink : outgoingLinks)
				outgoingLink.tail = nodeArray[i];
		}
		
		return new DynamicNetwork(nodeArray, originArray, destinationArray,
				linkArray, originConnectors, destinationConnectors);
	}
}
